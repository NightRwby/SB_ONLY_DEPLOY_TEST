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
    // 쓸데없이 개어렵노,,

    // 추가 로직 작성자리

    // 현재 선택된 버튼 css
    // const nav_btn = document.querySelectorAll(".side_item");
    // nav_btn.addEventListener('click',()=>{
    //     nav_btn.classList.add('active');
    // })




    // 이것이 엑조디아식 컴포넌트화의 꽃이다

    // 메인 컴포넌트 교체 스크립트
//    const freeCommunity_componentBtn = document.querySelector('.si_free');
//    const notice_componentBtn = document.querySelector('.si_notice');
//    const coverLetter_componentBtn = document.querySelector('.si_CL');
//    const HotIssue_componentBtn = document.querySelector('.si_IS');

    // 화면 변수 지정
    // querySelector는 앞에 선택자 똑바로 붙이기
    // 아니면 getElementByID 사용 권장
//    const communityHome_ComponentScreen = document.getElementById('communityHome_Component');
//    const freeCommunity_componentScreen = document.getElementById('freeCommunity_component');
//    const notice_componentScreen = document.getElementById('notice_component');
//    const cover_letter_componentScreen = document.getElementById('cover_letter_component');
//    const hotissue_componentScreen = document.getElementById('hotissue_component');



// 이거 진짜 나 새벽에 개열심히 만들었는데 진짜 하 타임리프 오니까 필요없노,,
// 눈물이 나온다,,,,,

//    if (freeCommunity_componentBtn &&
//        notice_componentBtn &&
//        coverLetter_componentBtn &&
//        HotIssue_componentBtn
//        ) {
//
//        // 기본값 홈 빼고 전부 none <<병신 화면을 가려야지 버튼을 처가리고 있네 << 담배피고 옴
//        // 커뮤니티로 진입하려면 무조건 커뮤 홈으로 이동하고나서 거쳐가도록 할 것
//        communityHome_ComponentScreen.style.display = "flex";
//        freeCommunity_componentScreen.style.display = "none";
//        notice_componentScreen.style.display = "none";
//        cover_letter_componentScreen.style.display = "none";
//        hotissue_componentScreen.style.display = "none";
//
//        // 각 컴포넌트 버튼 클릭시 뷰 노출 / 버튼 스타일 active
//        freeCommunity_componentBtn.addEventListener('click', () => {
//            communityHome_ComponentScreen.style.display = "none";
//
//            freeCommunity_componentScreen.style.display = "flex";
//            freeCommunity_componentBtn.classList.add('active');
//
//            notice_componentScreen.style.display = "none";
//            notice_componentBtn.classList.remove('active');
//
//            cover_letter_componentScreen.style.display = "none";
//            coverLetter_componentBtn.classList.remove('active');
//
//            hotissue_componentScreen.style.display = "none";
//            HotIssue_componentBtn.classList.remove('active');
//            return;
//        })
//        notice_componentBtn.addEventListener('click', () => {
//            communityHome_ComponentScreen.style.display = "none";
//
//            freeCommunity_componentScreen.style.display = "none";
//            freeCommunity_componentBtn.classList.remove('active');
//
//            notice_componentScreen.style.display = "flex";
//            notice_componentBtn.classList.add('active');
//
//            cover_letter_componentScreen.style.display = "none";
//            coverLetter_componentBtn.classList.remove('active');
//
//            hotissue_componentScreen.style.display = "none";
//            HotIssue_componentBtn.classList.remove('active');
//            return;
//        })
//        coverLetter_componentBtn.addEventListener('click', () => {
//            communityHome_ComponentScreen.style.display = "none";
//
//            freeCommunity_componentScreen.style.display = "none";
//            freeCommunity_componentBtn.classList.remove('active');
//
//            notice_componentScreen.style.display = "none";
//            notice_componentBtn.classList.remove('active');
//
//            cover_letter_componentScreen.style.display = "flex";
//            coverLetter_componentBtn.classList.add('active');
//
//            hotissue_componentScreen.style.display = "none";
//            HotIssue_componentBtn.classList.remove('active');
//            return;
//        })
//        HotIssue_componentBtn.addEventListener('click', () => {
//            communityHome_ComponentScreen.style.display = "none";
//
//            freeCommunity_componentScreen.style.display = "none";
//            freeCommunity_componentBtn.classList.remove('active');
//
//            notice_componentScreen.style.display = "none";
//            notice_componentBtn.classList.remove('active');
//
//            cover_letter_componentScreen.style.display = "none";
//            coverLetter_componentBtn.classList.remove('active');
//
//            hotissue_componentScreen.style.display = "flex";
//            HotIssue_componentBtn.classList.add('active');
//            return;
//        })
//    }

};