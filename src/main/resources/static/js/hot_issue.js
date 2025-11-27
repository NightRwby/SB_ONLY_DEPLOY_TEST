function init_hot_issue() {
    console.log("Hot Issue Logic Init (Prefix: h_)");

    // 더미 데이터
    const seed = [
        { id: 12, title: "프로야구 포스트시즌 판정 논란? 명장면 모음 & 토론", cat: "스포츠", author: "익명1", date: "2025-10-25", views: 1624, likes: 58, tags: ["#스포츠", "#야구"], ownerId: null },
        { id: 11, title: "요즘 밈 근황 10선  (틀면 나오는 그 노래 포함)", cat: "유머/밈", author: "익명2", date: "2025-10-24", views: 804, likes: 17, tags: ["#유머/밈"], ownerId: null },
        { id: 10, title: "GOTY 후보작 토론방: 올해의 게임은 무엇?", cat: "게임", author: "익명3", date: "2025-10-23", views: 1450, likes: 41, tags: ["#게임", "#GOTY"], ownerId: null },
        { id: 9, title: "아이돌 컴백 티저 공개—컨셉 평가 & 기대 포인트", cat: "연예인", author: "익명4", date: "2025-10-23", views: 972, likes: 29, tags: ["#연예인", "#컴백"], ownerId: null },
        { id: 8, title: "전월세/매매 체감담 모음: 체감 금리·거래량 어떰?", cat: "부동산", author: "익명5", date: "2025-10-22", views: 520, likes: 13, tags: ["#부동산", "#전세"], ownerId: null },
        { id: 7, title: "[공지] 핫이슈 게시판 이용 가이드", cat: "공지", author: "운영자", date: "2025-10-22", views: 734, likes: 12, tags: ["#공지"], ownerId: null },
        { id: 6, title: "10월 신작 애니 1화 라운드업", cat: "애니", author: "익명7", date: "2025-10-21", views: 1802, likes: 63, tags: ["#애니", "#신작"], ownerId: null },
    ];

    /* 상태 */
    let posts = [...seed];
    let currentPage = 1;
    const PAGE_SIZE = 10;
    const fmt = new Intl.NumberFormat();
    const MY_ID = "me"; // 내 ID

    /* DOM 요소 (Prefix h_) */
    const tbody = document.getElementById('h_tbody');
    const pag = document.getElementById('h_pagination');
    const catSel = document.getElementById('h_cat');
    const sortSel = document.getElementById('h_sort');
    const qInput = document.getElementById('h_q');
    const resetBtn = document.getElementById('h_resetBtn');
    const writeBtn = document.getElementById('h_writeBtn');

    /* 필터 & 렌더 */
    function applyFilters() {
        const cat = catSel.value.trim();
        const q = (qInput.value || "").trim().toLowerCase();
        let data = [...posts];

        if (cat) data = data.filter(d => d.cat === cat);
        if (q) {
            data = data.filter(d =>
                (d.title + " " + (d.tags || []).join(" ")).toLowerCase().includes(q)
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

        tbody.innerHTML = pageItems.map(row => {
            const mine = row.ownerId === MY_ID; // 내 글 여부
            return `
            <tr>
              <td class="h_user_tbl_num">${row.id}</td>
              <td class="h_user_tbl_title">
                <a href="javascript:void(0)" data-id="${row.id}" class="open-view">${row.title}</a>
                ${(row.tags || []).map(t => `<span class="h_tag" style="font-size:11px; color:#999; margin-left:4px;">${t}</span>`).join('')}
              </td>
              <td class="h_user_tbl_cart">${row.cat}</td>
              <td class="h_user_tbl_writer">${row.author}</td>
              <td class="h_user_tbl_date">${row.date}</td>
              <td class="h_user_tbl_visit">${fmt.format(row.views)}</td>
              <td class="h_user_tbl_like">${fmt.format(row.likes)}</td>
              <td class="h_user_tbl_state">
                ${mine
                    ? `<button class="h_manage-btn row-del" data-id="${row.id}">삭제</button>`
                    : `-`
                }
              </td>
            </tr>`;
        }).join('');

        // 상세보기 클릭 이벤트
        tbody.querySelectorAll('.open-view').forEach(a => {
            a.addEventListener('click', () => {
                alert("상세보기(모달) 연결 필요: ID " + a.dataset.id);
            });
        });

        // 페이지네이션
        const pages = [];
        const range = (f, t) => Array.from({ length: t - f + 1 }, (_, i) => f + i);
        const neighbors = 1;
        let sp = Math.max(1, currentPage - neighbors);
        let ep = Math.min(totalPages, currentPage + neighbors);

        if (sp > 1) pages.push(1, '...');
        pages.push(...range(sp, ep));
        if (ep < totalPages) pages.push('...', totalPages);

        pag.innerHTML = `
            <button class="h_page_btn" ${currentPage === 1 ? 'disabled' : ''} data-page="${currentPage - 1}">이전</button>
            ${pages.map(p => p === '...'
                ? `<span class="h_page_btn" style="pointer-events:none;border-style:dashed;">...</span>`
                : `<button class="h_page_btn ${p === currentPage ? 'active' : ''}" data-page="${p}">${p}</button>`
            ).join('')}
            <button class="h_page_btn" ${currentPage === totalPages ? 'disabled' : ''} data-page="${currentPage + 1}">다음</button>
        `;
    }

    // 페이지네이션 클릭
    pag.addEventListener('click', (e) => {
        const btn = e.target.closest('.h_page_btn');
        if (!btn) return;
        const page = Number(btn.dataset.page);
        if (!isNaN(page)) { currentPage = page; render(); }
    });

    [catSel, sortSel].forEach(el =>
        el.addEventListener('change', () => { currentPage = 1; render(); })
    );
    qInput.addEventListener('input', () => { currentPage = 1; render(); });

    // 초기화 버튼
    resetBtn.addEventListener('click', () => {
        catSel.value = "";
        sortSel.value = "latest";
        qInput.value = "";
        document.querySelectorAll('.h_chip').forEach(c => c.classList.remove('active'));
        currentPage = 1;
        render();
    });

    // 칩 클릭
    document.querySelectorAll('.h_chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const val = chip.dataset.chip;
            const willActive = !chip.classList.contains('active');
            document.querySelectorAll('.h_chip').forEach(c => c.classList.remove('active'));
            if (willActive) chip.classList.add('active');
            catSel.value = willActive ? val : "";
            currentPage = 1;
            render();
        });
    });

    // 글쓰기 버튼 (임시 alert)
    writeBtn.addEventListener('click', () => {
        alert("글쓰기 모달을 연결해주세요.");
    });

    render();
}