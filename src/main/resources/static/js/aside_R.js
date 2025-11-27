// js/aside_R.js

// [1] 전역 변수 설정 (함수 밖으로 뺌)
let myOnlineStatus = true;

// [2] 초기화 및 리렌더링 함수 (외부에서 호출 가능)
function initAsideR() {
    console.log("initAsideR 실행됨");

    // HTML에서 넘어온 데이터 가져오기 (exodia.html 하단 스크립트 참고)
    const profile = window.myProfile || {};

    // DOM 요소 가져오기
    const pName = document.getElementById('pName');
    const pImg = document.getElementById('p_img');
    const pStateText = document.getElementById('pStateText');
    const pDot = document.getElementById('pDot');

    // 1. 프로필 데이터 적용
    if (profile.email && profile.nickName) {
        // 1) 닉네임 설정
        if (pName) pName.textContent = profile.nickName;

        // 2) 프사 (초성) 설정
        if (pImg) {
            pImg.textContent = getKoreanInitials(profile.nickName);
            // 만약 이미지 URL이 있다면 아래 주석 해제
            // if (profile.profileImageUrl) { pImg.style.backgroundImage = `url(${profile.profileImageUrl})`; pImg.textContent = ''; }
        }

        // 3) 초기 온라인 상태 UI 업데이트
        updateOnlineStatusUI();

        // 4) 클릭 이벤트 연결 (중복 방지를 위해 onclick 사용)
        if (pDot) {
            pDot.onclick = function() {
                toggleOnlineStatus();
            };
        }

    } else {
        // 비로그인 상태 처리
        if (pName) pName.textContent = "로그인이 필요합니다";
        if (pImg) pImg.textContent = "?";
        // 비로그인 시 오프라인 처리
        if (pDot) {
            pDot.classList.remove('online');
            pDot.classList.add('offline');
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

function updateOnlineStatusUI() {
    const pStateText = document.getElementById('pStateText');
    const pDot = document.getElementById('pDot');

    if (!pStateText || !pDot) return;

    if (myOnlineStatus) {
        pDot.classList.remove('offline');
        pDot.classList.add('online');
        pStateText.textContent = '온라인';
    } else {
        pDot.classList.remove('online');
        pDot.classList.add('offline');
        pStateText.textContent = '오프라인';
    }
}

// [4] 한글 초성 추출 함수
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

// [5] 스크롤 따라다니는 기능
function initAsideScroll() {
    const asideR = document.getElementById('aside_R');
    const box2 = document.getElementById('followscr2');

    if (!asideR || !box2) return;

    box2.style.position = "absolute";
    box2.style.top = "0px";
    box2.style.width = "100%";

    let currentY = 0;

    function animate() {
        let targetY = window.scrollY + (window.scrollY * 0.16);
        currentY += (targetY - currentY) * 0.05;

        const maxScroll = asideR.offsetHeight - box2.offsetHeight;
        let finalY = Math.min(Math.max(0, currentY), maxScroll);

        box2.style.top = finalY + "px";
        requestAnimationFrame(animate);
    }
    animate();
}