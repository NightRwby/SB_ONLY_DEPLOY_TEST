// <!-- aside-l 스크롤 스크립트 -->

function initAsideL() {
    const asideL = document.getElementById("aside_L");
    const box = document.getElementById("followscr");

    // 요소 없으면 종료
    if (!asideL || !box) return;

    // 초기화: 박스를 absolute로 만들고 위치 잡기
    box.style.position = "absolute";
    box.style.top = "0px";
    box.style.width = "100%"; // 너비 깨짐 방지

    let targetY = 0;
    let currentY = 0;

    function smoothFollow() {
        // 목표 위치: 현재 스크롤 위치 + 50px (여백)
        targetY = window.scrollY + 50;

        // 부드러운 이동 (보간법)
        currentY += (targetY - currentY) * 0.05; // 속도 0.04 -> 0.05 살짝 올림

        // 이동 범위 제한 계산
        // asideL(부모) 높이 - box(따라다니는 애) 높이 - 하단 여백(40)
        const maxScroll = asideL.offsetHeight - box.offsetHeight - 40;

        // 0보다 작을 순 없음 (위로 튀어나감 방지)
        const safeMax = Math.max(0, maxScroll);

        // 최종 위치 결정 (0 ~ safeMax 사이)
        let finalY = Math.min(Math.max(currentY, 0), safeMax);

        box.style.top = finalY + "px";

        requestAnimationFrame(smoothFollow);
    }
    smoothFollow();
};