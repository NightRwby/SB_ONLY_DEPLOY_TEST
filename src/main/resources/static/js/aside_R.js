// [1] 전역 변수 설정
let myOnlineStatus = true;

// [2] 초기화 및 리렌더링 함수
function initAsideR() {
    console.log("initAsideR 실행됨");

    // HTML에서 window.myProfile에 주입된 데이터 가져오기
    const profile = window.myProfile || {};

    // DOM 요소 가져오기
    const pName = document.getElementById('pName');
    const pImg = document.getElementById('p_img');
    const pMessage = document.getElementById('pMessage');
    const pStateText = document.getElementById('pStateText');
    const pDot = document.getElementById('pDot');

    // 1. 프로필 데이터 적용
    if (profile.email && profile.nickName) {
        // 1) 닉네임
        if (pName) pName.textContent = profile.nickName;

        // 2) 상태 메시지 처리 (null, "null", 공백 모두 체크)
        if (pMessage) {
            let msg = profile.stateMessage;

            // 유효한 메시지인지 검증 (문자열 변환 후 trim)
            const isValidMsg = msg && String(msg).trim() !== '' && String(msg) !== 'null';

            if (isValidMsg) {
                // 줄바꿈 문자(\n)가 있다면 <br>로 변환해서 표시
                pMessage.innerHTML = String(msg).replace(/\n/g, '<br>');
            } else {
                pMessage.textContent = "상태 메시지가 없습니다.";
            }
        }

        // 3) 프로필 이미지 처리 (null, "null", 공백 모두 체크)
        if (pImg) {
            let imgUrl = profile.profileImageUrl;

            // 유효한 이미지 URL인지 검증
            const hasImage = imgUrl && String(imgUrl).trim() !== '' && String(imgUrl) !== 'null';

            if (hasImage) {
                // 이미지가 있으면: 텍스트 비우고, 배경이미지 설정
                pImg.textContent = '';
                // background 속성 한 줄로 처리 (이미지 경로, 센터 정렬, 꽉 채우기, 반복 없음)
                pImg.style.background = `url('${imgUrl}') center/cover no-repeat`;
                pImg.style.boxShadow = 'none'; // 이미지일 때는 그림자 제거 (선택사항)
            } else {
                // 이미지가 없으면: CSS 그라데이션 복구 (인라인 스타일 제거)
                pImg.style.background = '';
                pImg.style.backgroundImage = '';
                pImg.textContent = getKoreanInitials(profile.nickName);
                pImg.style.boxShadow = ''; // CSS 그림자 복구
            }
        }

        // 4) 온라인 상태 초기값 적용
        const status = profile.onlineStatus || 'online';
        myOnlineStatus = (status === 'online');
        updateOnlineStatusUI();

        // 5) 상태 클릭 시 토글 이벤트
        if (pDot) {
            pDot.onclick = function() {
                toggleOnlineStatus();
            };
        }

    } else {
        // 비로그인 상태 처리
        if (pName) pName.textContent = "로그인이 필요합니다";
        if (pMessage) pMessage.textContent = "로그인 후 다양한 활동을 즐겨보세요.";

        if (pImg) {
            pImg.textContent = "?";
            pImg.style.background = ''; // CSS 기본 스타일(그라데이션 등) 따름
        }
        if (pDot) {
            pDot.className = 'p_status_dot offline';
        }
        if (pStateText) pStateText.textContent = '오프라인';
    }

    // 2. 스크롤 기능 시작
    initAsideScroll();
}

// [3] 상태 토글 로직
function toggleOnlineStatus() {
    myOnlineStatus = !myOnlineStatus;
    updateOnlineStatusUI();
}

// [4] UI 업데이트
function updateOnlineStatusUI() {
    const pStateText = document.getElementById('pStateText');
    const pDot = document.getElementById('pDot');

    if (!pStateText || !pDot) return;

    pDot.className = 'p_status_dot';

    if (myOnlineStatus) {
        pDot.classList.add('online');
        pStateText.textContent = '온라인';
    } else {
        pDot.classList.add('offline');
        pStateText.textContent = '오프라인';
    }
}

// [5] 한글 초성 추출 함수
function getKoreanInitials(text) {
    if (!text || typeof text !== 'string') return '';
    const CHOSUNG = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'];

    const firstChar = text.charAt(0);
    const charCode = firstChar.charCodeAt(0);

    if (charCode >= 44032 && charCode <= 55203) {
        const index = Math.floor((charCode - 44032) / 588);
        return CHOSUNG[index];
    }
    return firstChar;
}

// [6] 스크롤 따라다니는 기능
function initAsideScroll() {
    const asideR = document.getElementById('aside_R');
    const box2 = document.getElementById('followscr2');

    if (!asideR || !box2) return;

    // 부모 relative 설정
    asideR.style.position = "relative";

    box2.style.position = "absolute";
    box2.style.top = "0px";
    box2.style.width = "100%";

    let currentY = 0;

    function animate() {
        // 1. 페이지 전체 스크롤 비율(Progress) 계산 (0.0 ~ 1.0)
        const scrollTop = window.scrollY; // 현재 스크롤 된 위치
        const docHeight = document.body.scrollHeight; // 문서 전체 높이
        const winHeight = window.innerHeight; // 브라우저 화면 높이

        // 스크롤 가능한 전체 길이
        const maxPageScroll = docHeight - winHeight;

        // 현재 스크롤 비율 (예: 0.5는 문서 중간)
        let scrollRatio = 0;
        if (maxPageScroll > 0) {
            scrollRatio = scrollTop / maxPageScroll;
        }

        // 2. aside_R 내부에서 박스가 움직일 수 있는 최대 거리 계산
        const asideHeight = asideR.offsetHeight;
        const boxHeight = box2.offsetHeight;

        // 박스가 움직일 수 있는 여유 공간 (트랙 길이)
        const maxBoxMove = asideHeight - boxHeight;

        // 3. 목표 위치 설정 (여유 공간 * 스크롤 비율)
        // 비율이 0이면 0px (상단)
        // 비율이 1이면 maxBoxMove (하단)
        let targetY = maxBoxMove * scrollRatio;

        // 4. 부드러운 이동 (보간) - 기존 느낌 유지
        currentY += (targetY - currentY) * 0.05;

        // 5. 경계 체크 (수학적으로는 넘지 않지만 안전장치)
        // 0보다 작을 수 없고, maxBoxMove보다 클 수 없음
        let finalY = Math.min(Math.max(0, currentY), maxBoxMove);

        box2.style.top = finalY + "px";

        requestAnimationFrame(animate);
    }

    animate();
}