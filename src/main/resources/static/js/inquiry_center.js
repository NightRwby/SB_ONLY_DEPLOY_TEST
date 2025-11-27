        new Swiper(".mySwiper", {
            direction: "vertical",   // 세로 모드
            loop: true,              // 무한 루프
            autoplay: {
                delay: 3000,           // 3초마다 변경
            },
            allowTouchMove: false    // 마우스로는 넘기지 못하게
        });




        //q카드 숨김버튼

        document.querySelector(".loadMoreBtn").addEventListener("click", function () {
            const hiddenCards = document.querySelectorAll(".Q-card.hidden");
            hiddenCards.forEach(card => card.style.display = "block");
            this.style.display = "none"; // 버튼 숨기기
        });


        const 왜안됨버튼 = document.querySelectorAll('.s5-card');
        const 이러면됨버튼 = [왜안됨버튼[0], 왜안됨버튼[1], 왜안됨버튼[2]]
        const 이모티콘 = document.querySelectorAll('.s5-card-title .material-symbols-outlined');
        const 진짜바뀜 = 이모티콘[0, 2];

        이러면됨버튼.forEach((btn) => {
            btn.addEventListener('mouseover', () => {
                // 호버된 버튼(btn) 내에서
                // 클래스 선택자를 수정하여 해당 아이콘 요소를 찾기
                const 아이콘 = btn.querySelector('.material-symbols-outlined');
                아이콘.style.color = '#3EE9B9';
            });
            // 원래 색상으로
            btn.addEventListener('mouseout', () => {
                const 아이콘 = btn.querySelector('.material-symbols-outlined');
                // 초기 색상으로 설정
                아이콘.style.color = '';
            });
        });



        // 폼 이벤트 추가
        const searchForm = document.querySelector('.search-form');

        searchForm.addEventListener('focusin', () => {
            searchForm.style.border = '2px solid #06b6d4'
        })
        searchForm.addEventListener('focusout', () => {
            searchForm.style.border = '1px solid #e6e6e6'
        })


        // 클릭하는 동안 이미지 변경
        const mush_CON = document.querySelectorAll('.mush-con');

        mush_CON.forEach(con => {
            const img = con.querySelector('.mush_con_image');

            const winkMush = '/img/찡긋버섯.png';
            const miaMush = '/img/버섯미아.png';

            con.addEventListener('mousedown', () => {
                img.src = winkMush;
            });

            con.addEventListener('mouseup', () => {
                img.src = miaMush;
            });

            con.addEventListener('mouseleave', () => {
                img.src = miaMush;
            });

        });