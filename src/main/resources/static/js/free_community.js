// free_community.js

let modalPreview;
let updateModalPreview;
let currentBoardId = null;
let currentBoardData = null; // [추가] 현재 보고 있는 게시글의 전체 데이터를 저장할 변수

// [상세보기] 버튼들
const deleteBtn = document.getElementById('detail_delete');
const repairBtn = document.getElementById('detail_update');
const likeBtn = document.querySelector('.likebtn');

function initfree() {
    console.log("게시판 로직 초기화 (Update Logic Added)");

    // 1. 툴바 및 검색 로직
    const keywordInput = document.getElementById('tool-input');
    const resetBtn = document.getElementById('resetBtn');
    const chipBtns = document.querySelectorAll('.chip');
    const categorySelect = document.getElementById('tool-select');
    const searchForm = document.querySelector('.toolbar_form');

    // 칩 클릭 -> 검색
    if (chipBtns && keywordInput) {
        chipBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const chipText = btn.innerText.trim();
                keywordInput.value = chipText;
                if (searchForm) searchForm.submit();
            });
        });
    }

    // 초기화
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            window.location.href = '/community/free/list';
        });
    }

    if (categorySelect) categorySelect.addEventListener('change', () => searchForm.submit());
    const sortSelect = document.getElementById('tool-select2');
    if (sortSelect) sortSelect.addEventListener('change', () => searchForm.submit());

    // 글쓰기 버튼
    const writeBtn = document.getElementById('writeBtn');
    if (writeBtn) writeBtn.addEventListener('click', () => openWriteModal());

    // 상세보기 클릭
    const detailLinks = document.querySelectorAll('.user_tbl_title a');
    if (detailLinks) {
        detailLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const href = link.getAttribute('href');
                const id = href.split('id=')[1];
                if(id) openDetailModal(id);
            });
        });
    }
}

function initModal() {
    // 닫기 버튼 공통
    document.querySelectorAll('.btn_modal_close').forEach(btn => {
        btn.addEventListener('click', function () {
            const wrapper = this.closest('.modal_screen, .modal_update_screen, .modal_detail_screen');
            if (wrapper) wrapper.style.display = 'none';
        });
    });

    // [글쓰기] 관련
    const writeUpImg = document.querySelector('.modal_screen .upload_wrap');
    const writeFileInput = document.getElementById('wImages');
    const writeClearBtn = document.getElementById('clearImages');
    const writeTextarea = document.getElementById('modal_textarea');
    modalPreview = document.getElementById('modal_file_preview');

    if (writeTextarea) writeTextarea.addEventListener('input', function() { this.style.height = this.scrollHeight + 'px'; });
    if (writeUpImg && writeFileInput) {
        writeUpImg.addEventListener('click', () => writeFileInput.click());
        writeFileInput.addEventListener('change', () => showPreview(writeFileInput.files, modalPreview));
    }
    if (writeClearBtn) {
        writeClearBtn.addEventListener('click', () => {
            writeFileInput.value = "";
            modalPreview.innerHTML = "";
            modalPreview.style.display = 'none';
        });
    }

    // [수정하기] 관련
    const updateUpImg = document.querySelector('.modal_update_screen .upload_wrap');
    const updateFileInput = document.getElementById('update_wImages');
    const updateClearBtn = document.getElementById('update_clearImages');
    const updateTextarea = document.getElementById('update_modal_textarea');
    updateModalPreview = document.getElementById('update_modal_file_preview');

    if (updateTextarea) updateTextarea.addEventListener('input', function() { this.style.height = this.scrollHeight + 'px'; });

    if (updateUpImg && updateFileInput) {
        updateUpImg.addEventListener('click', () => updateFileInput.click());
        updateFileInput.addEventListener('change', () => showPreview(updateFileInput.files, updateModalPreview));
    }

    // 수정 폼 이미지 초기화 (기존 이미지도 화면에서 지움 - 실제 삭제는 구현 복잡하므로 시각적 처리만)
    if (updateClearBtn) {
            updateClearBtn.addEventListener('click', () => {
                // ▼▼▼ [이 부분이 실행되어야 합니다] ▼▼▼
                const flagInput = document.getElementById('clearExistingFlag');
                if (flagInput) {
                    flagInput.value = 'true';
                    console.log(">> JS Log: Flag set to", flagInput.value);
                }

                updateFileInput.value = "";
                if(updateModalPreview) {
                    updateModalPreview.innerHTML = "";
                    updateModalPreview.style.display = 'none';
                }
            });
        }


    // 삭제
    if (deleteBtn) {
        deleteBtn.addEventListener('click', () => {
            if (!currentBoardId) return;
            if (confirm("정말 삭제하시겠습니까?")) {
                fetch(`/community/api/delete/${currentBoardId}`, { method: 'DELETE' })
                .then(res => {
                    if (res.ok) {
                        alert("삭제되었습니다.");
                        window.location.reload();
                    } else {
                        alert("삭제 실패");
                    }
                });
            }
        });
    }

    // 수정 모달 열기
    if (repairBtn) {
        repairBtn.addEventListener('click', () => {
            document.querySelector('.modal_detail_screen').style.display = "none";
            openUpdateModal(); // 수정 모달 열기 함수 호출
        });
    }

    // 추천 토글
    if (likeBtn) {
        likeBtn.addEventListener('click', () => {
            if (!currentBoardId) return;

            const storageKey = 'liked_' + currentBoardId;
            const isLiked = localStorage.getItem(storageKey);
            const status = !isLiked; // true면 추천, false면 취소

            fetch(`/community/api/like/${currentBoardId}?status=${status}`, { method: 'POST' })
                .then(res => res.json())
                .then(newCount => {
                    document.getElementById('detailLikes').innerText = "추천수 : " + newCount;

                    if (status) {
                        likeBtn.classList.add('active');
                        likeBtn.innerText = "추천 취소";
                        localStorage.setItem(storageKey, 'true');
                    } else {
                        likeBtn.classList.remove('active');
                        likeBtn.innerText = "추천하기 !";
                        localStorage.removeItem(storageKey);
                    }
                })
                .catch(err => console.error("추천 처리 실패", err));
        });
    }
}

// =========================================================
// Helper Functions
// =========================================================

function openWriteModal() {
    const s = document.querySelector('.modal_screen');
    if (s) s.style.display = 'block';
}

function openDetailModal(id) {
    currentBoardId = id;
    const detailScreen = document.querySelector('.modal_detail_screen');
    const likeBtn = document.querySelector('.likebtn');

    document.getElementById('detailTitle').innerText = "로딩중...";
    const imgContainer = document.getElementById('detailImages');
    if (imgContainer) imgContainer.innerHTML = "";

    // 1. [이슈 1 해결] 추천 버튼 상태를 일단 '초기화' (안 누른 상태로)
    if (likeBtn) {
        likeBtn.classList.remove('active');
        likeBtn.innerText = "추천하기 !";
    }

    fetch(`/community/api/detail/${id}`)
        .then(response => response.json())
        .then(data => {
            currentBoardData = data; // [중요] 수정 폼을 위해 데이터 저장


        //  내 이름과 작성자 이름이 다르면 수정 삭제 버튼 숨기기 간단한 보안처리
            const myName = document.getElementById('pName');
            const writer = data.writer;
            if (myName&&writer) {
                if(myName.textContent === data.writer) {
                console.log("myname : ",myName,"writer : ",data.writer);
                    deleteBtn.style.display="inline-block";
                    repairBtn.style.display="inline-block";
                }
                else {
                console.log("myname : ",myName.textContent,"writer : ",data.writer);
                    deleteBtn.style.display="none";
                    repairBtn.style.display="none";
                }
            }



            document.getElementById('detailTitle').innerText = data.title;
            document.getElementById('detailWriter').innerText = "작성자 : " + (data.writer || "익명");
            document.getElementById('detailDate').innerText = "날짜 : " + (data.createdDate ? data.createdDate.split('T')[0] : "");
            document.getElementById('detailCategory').innerText = "카테고리 : " + (data.category || "");
            document.getElementById('detailContent').innerText = data.content;
            document.getElementById('detailViews').innerText = "조회수 : " + data.views;
            document.getElementById('detailLikes').innerText = "추천수 : " + data.likes;

            // 이미지 렌더링
            if (data.filepath && imgContainer) {
                const paths = data.filepath.split(',');
                paths.forEach(path => {
                    if(path.trim() !== "") {
                        const img = document.createElement('img');
                        img.src = path.trim();
                        img.style.maxWidth = "100%";
                        img.style.borderRadius = "8px";
                        img.style.border = "1px solid #eee";
                        imgContainer.appendChild(img);
                    }
                });
            }

            // 2. [이슈 1 해결] LocalStorage 확인 후 '누른 상태'면 그때 바꿈
            if (localStorage.getItem('liked_' + id)) {
                likeBtn.classList.add('active');
                likeBtn.innerText = "추천 취소";
            }

            if (detailScreen) detailScreen.style.display = 'block';
        })
        .catch(err => {
            console.error(err);
            alert("게시글 정보를 불러오는데 실패했습니다.");
        });

}

// [이슈 2 해결] 수정 모달 열 때 기존 이미지 보여주기
function openUpdateModal() {
    const updateScreen = document.querySelector('.modal_update_screen');

    // [추가] 폼 열 때마다 삭제 플래그를 false로 초기화 (재사용 대비)
    const clearFlagInput = document.getElementById('clearExistingFlag');
    if (clearFlagInput) clearFlagInput.value = 'false';

    // 아까 저장해둔 데이터(currentBoardData)가 없으면 리턴
    if (!currentBoardData) return;

    document.getElementById('update_id').value = currentBoardData.id;
    document.getElementById('update_modal_input').value = currentBoardData.title;
    document.getElementById('update_modal_textarea').value = currentBoardData.content;

    // 카테고리 선택
    const cleanCat = (currentBoardData.category || "").trim();
    const select = document.getElementById('update_modal_select');
    if(select) select.value = cleanCat;

    // ★ [핵심] 기존 이미지 미리보기에 그리기
    if (updateModalPreview) {
        updateModalPreview.innerHTML = ""; // 초기화
        updateModalPreview.style.display = 'none';

        if (currentBoardData.filepath) {
            const paths = currentBoardData.filepath.split(',');
            paths.forEach(path => {
                if(path.trim() !== "") {
                    const img = document.createElement('img');
                    img.src = path.trim();
                    // 스타일 (기존 showPreview 함수와 동일하게)
                    img.style.width = "80px";
                    img.style.height = "50px";
                    img.style.objectFit = "cover";
                    img.style.borderRadius = "6px";
                    img.style.border = "1px solid #e5e7eb";
                    img.style.marginLeft = "5px";

                    updateModalPreview.appendChild(img);
                }
            });
            // 이미지가 하나라도 있으면 보이기
            if (updateModalPreview.children.length > 0) {
                updateModalPreview.style.display = 'flex';
            }
        }
    }

    if (updateScreen) updateScreen.style.display = 'block';
}

function showPreview(files, targetPreview) {
    if (!targetPreview) return;
    // 기존 이미지가 있다면 유지하고 뒤에 추가할지, 아니면 놔둘지 결정
    // 여기선 '새로 추가된 파일'만 보여주는 게 아니라, 기존 DOM에 appendChild 하므로 같이 보임

    const existingFiles = Array.from(targetPreview.querySelectorAll('img')).map(img => img.dataset.name); // 기존 이미지는 dataset.name이 없을 수도 있음

    Array.from(files).forEach(file => {
        if (!file.type.startsWith("image/")) return;
        // 중복 방지는 이름으로 체크 (기존 DB이미지는 이름이 없어서 패스됨)
        if (existingFiles.includes(file.name)) return;

        const img = document.createElement("img");
        img.src = URL.createObjectURL(file);
        img.dataset.name = file.name;
        img.style.width = "80px";
        img.style.height = "50px";
        img.style.objectFit = "cover";
        img.style.borderRadius = "6px";
        img.style.border = "1px solid #e5e7eb";
        img.style.marginLeft = "5px";

        targetPreview.appendChild(img);
    });

    if (targetPreview.children.length > 0) {
        targetPreview.style.display = 'flex';
    }
}