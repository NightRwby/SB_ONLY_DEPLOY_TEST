function init_cover_letter () {
    /* 더미 데이터 (기존 유지) */
    const seed = [
        { id: 12, title: "합격 자소서 공개 (항목별)", cat: "합격후기", author: "익명1", date: "2025-10-25", views: 1624, likes: 58, tags: ["#합격후기"], body: "요약 본문 예시", images: [] },
        { id: 11, title: "불합 피드백 받고 수정한 버전", cat: "불합격피드백", author: "익명2", date: "2025-10-24", views: 804, likes: 17, tags: ["#불합격피드백"], body: "본문 예시", images: [] },
        // ... (나머지 데이터 유지) ...
        { id: 1, title: "신입 공통 1000자 4문항 템플릿", cat: "자소서템플릿", author: "운영자", date: "2025-10-17", views: 2100, likes: 71, tags: ["#자소서템플릿", "#신입"], body: "본문 예시", images: [] },
    ];

    /* 상태 */
    let posts = [...seed];
    let currentPage = 1;
    const PAGE_SIZE = 10;
    const fmt = new Intl.NumberFormat();
    let uploadImages = [];
    let editingId = null;

    /* DOM 요소 가져오기 (ID 앞에 c_ 추가됨) */
    const tbody = document.getElementById('c_tbody');
    const pag = document.getElementById('c_pagination');
    const catSel = document.getElementById('c_cat');
    const sortSel = document.getElementById('c_sort');
    const qInput = document.getElementById('c_q');
    const resetBtn = document.getElementById('c_resetBtn');
    const writeBtn = document.getElementById('c_writeBtn');

    // 모달 관련은 기존 ID를 그대로 쓴다면 유지, 바꿨다면 수정 필요.
    // 여기서는 메인 리스트 로직 위주로 c_ 적용됨을 가정.
    const modal = document.getElementById('modalBackdrop');
    // ... (모달 관련 DOM은 HTML에 c_를 안 붙였다면 기존 유지)

    /* 유틸: 내 이름 가져오기 */
    const getMyName = () => "익명"; // 간단화

    /* ---------- 필터 & 렌더 ---------- */
    function applyFilters() {
        const cat = catSel.value.trim();
        const q = (qInput.value || "").trim().toLowerCase();
        let data = [...posts];

        if (cat) data = data.filter(d => d.cat === cat);
        if (q) {
            data = data.filter(d =>
                (d.title + " " + (d.tags || []).join(" ") + " " + (d.body || "")).toLowerCase().includes(q)
            );
        }

        const sort = sortSel.value;
        if (sort === 'latest')
            data.sort((a, b) => new Date(b.date) - new Date(a.date) || b.id - a.id);
        else if (sort === 'views')
            data.sort((a, b) => b.views - a.views);
        else if (sort === 'likes')
            data.sort((a, b) => b.likes - a.likes);

        return data;
    }

    function render() {
        const data = applyFilters();
        const total = data.length;
        const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        const start = (currentPage - 1) * PAGE_SIZE;
        const pageItems = data.slice(start, start + PAGE_SIZE);
        const myName = getMyName();

        // 테이블 클래스도 c_ 접두사 적용
        tbody.innerHTML = pageItems.map(row => {
            const mine = row.author === myName;
            return `
    <tr>
      <td class="c_user_tbl_num">${row.id}</td>
      <td class="c_user_tbl_title">
        <a href="javascript:void(0)" data-id="${row.id}" class="open-view">${row.title}</a>
        ${(row.tags || []).map(t => `<span class="c_tag">${t}</span>`).join('')}
      </td>
      <td class="c_user_tbl_cart">${row.cat}</td>
      <td class="c_user_tbl_writer">${row.author}</td>
      <td class="c_user_tbl_date">${row.date}</td>
      <td class="c_user_tbl_visit">${fmt.format(row.views)}</td>
      <td class="c_user_tbl_like">${fmt.format(row.likes)}</td>
      <td class="c_user_tbl_state">
        ${mine
                    ? `
          <button class="c_manage-btn row-edit" data-id="${row.id}">수정</button>
          <button class="c_manage-btn row-del" data-id="${row.id}">삭제</button>
        `
                    : `-`
                }
      </td>
    </tr>`;
        }).join('');

        // 상세 보기 등 이벤트
        tbody.querySelectorAll('.open-view').forEach(a => {
            a.addEventListener('click', () => {
                alert("상세보기(모달) 연결 필요: ID " + a.dataset.id);
            });
        });

        // 페이지네이션 (클래스 c_page_btn)
        const pages = [];
        const range = (f, t) => Array.from({ length: t - f + 1 }, (_, i) => f + i);
        const neighbors = 1;
        let sp = Math.max(1, currentPage - neighbors);
        let ep = Math.min(totalPages, currentPage + neighbors);

        if (sp > 1) pages.push(1, '...');
        pages.push(...range(sp, ep));
        if (ep < totalPages) pages.push('...', totalPages);

        pag.innerHTML = `
    <button class="c_page_btn" ${currentPage === 1 ? 'disabled' : ''} data-page="${currentPage - 1}">이전</button>
    ${pages.map(p => p === '...'
            ? `<span class="c_page_btn" style="pointer-events:none;border-style:dashed;">...</span>`
            : `<button class="c_page_btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`
        ).join('')}
    <button class="c_page_btn" ${currentPage === totalPages ? 'disabled' : ''} data-page="${currentPage + 1}">다음</button>
  `;
    }

    // 페이지네이션 클릭 이벤트
    pag.addEventListener('click', (e) => {
        const btn = e.target.closest('.c_page_btn');
        if (!btn) return;
        const page = Number(btn.dataset.page);
        if (!isNaN(page)) { currentPage = page; render(); }
    });

    [catSel, sortSel].forEach(el =>
        el.addEventListener('change', () => { currentPage = 1; render(); })
    );
    qInput.addEventListener('input', () => { currentPage = 1; render(); });

    resetBtn.addEventListener('click', () => {
        catSel.value = "";
        sortSel.value = "latest";
        qInput.value = "";
        document.querySelectorAll('.c_chip').forEach(c => c.classList.remove('active'));
        currentPage = 1;
        render();
    });

    // 칩 클릭 이벤트 (클래스 c_chip)
    document.querySelectorAll('.c_chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const val = chip.dataset.chip;
            const willActive = !chip.classList.contains('active');
            document.querySelectorAll('.c_chip').forEach(c => c.classList.remove('active'));
            if (willActive) chip.classList.add('active');
            catSel.value = willActive ? val : "";
            currentPage = 1;
            render();
        });
    });

    /* 초기 렌더 */
    render();
}