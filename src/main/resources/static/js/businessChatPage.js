document.addEventListener('DOMContentLoaded', () => {
    // DOM 참조
    const tabBarEl = document.getElementById('tab-bar');

    const roomListEl = document.getElementById('chat-room-list');
    const newRoomBtn = document.getElementById('new-room');
    const roomSearchInput = document.getElementById('room-search');

    const chatAreaWrapper = document.getElementById('chat-area-wrapper');
    const emptyState = document.getElementById('empty-state');

    const onlineMemberListEl = document.getElementById('online-member-list');
    const offlineMemberListEl = document.getElementById('offline-member-list');
    const addMemberBtn = document.getElementById('add-member-btn');
    const memberSearchInput = document.getElementById('member-search');

    // -----------------------------
    // 상태 (방 / 메시지 / 멤버 / 탭 데이터)
    // -----------------------------
    let rooms = [
        {
            id: 'room-1',
            name: '프로젝트 A - 팀 협업방',
            members: [
                { id: 'u1', name: '김민수', role: '프로젝트 매니저', status: 'online' },
                { id: 'u2', name: '이수진', role: '팀 리더', status: 'online' },
                { id: 'u3', name: '박준호', role: '개발자', status: 'online' },
                { id: 'u4', name: '최지은', role: '디자이너', status: 'busy' },
                { id: 'u5', name: '남규민', role: '개발팀 막내', status: 'offline' }
            ],
            messages: [
                {
                    id: 'm1',
                    sender: '김민수',
                    text: '회의 일정을 생성했습니다.',
                    time: '09:10'
                },
                {
                    id: 'm2',
                    sender: '이수진',
                    text: '회의 전까지 자료 정리 부탁드립니다.',
                    time: '09:15'
                }
            ]
        },
        {
            id: 'room-2',
            name: '디자인 리뷰 채팅방',
            members: [
                { id: 'u2', name: '이수진', role: '팀 리더', status: 'online' },
                { id: 'u4', name: '최지은', role: '디자이너', status: 'online' }
            ],
            messages: [
                {
                    id: 'm3',
                    sender: '최지은',
                    text: '새로운 메인 배너 초안 올렸어요.',
                    time: '11:00'
                }
            ]
        }
    ];

    let activeRoomId = null;     // 현재 보고 있는 방
    let openRoomTabs = [];       // 탭으로 열려 있는 방 id 배열
    let roomSearchText = '';
    let memberSearchText = '';

    const MY_NAME = '나'; // 내 이름(프론트 기준)

    // 업로드 예정 파일들 (현재 선택된 방 기준)
    let pendingFiles = [];

    // -----------------------------
    // 유틸
    // -----------------------------
    const getCurrentTime = () =>
        new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

    const getRoomById = (id) => rooms.find(r => r.id === id);

    const escapeHtml = (str) =>
        str.replace(/[&<>"']/g, m => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[m]));

    // -----------------------------
    // 탭 렌더링
    // -----------------------------
    function renderTabBar() {
        if (!tabBarEl) return;
        tabBarEl.innerHTML = '';

        if (!openRoomTabs.length) {
            tabBarEl.style.display = 'none';
            return;
        }
        tabBarEl.style.display = 'flex';

        openRoomTabs.forEach(roomId => {
            const room = getRoomById(roomId);
            if (!room) return;

            const tab = document.createElement('div');
            tab.className = 'tab';
            if (roomId === activeRoomId) {
                tab.classList.add('active');
            }
            tab.dataset.roomId = roomId;

            tab.innerHTML = `
                <span class="tab-title">${escapeHtml(room.name)}</span>
                <button class="tab-close" title="탭 닫기">✕</button>
            `;

            // 탭 클릭 → 방 활성화
            tab.addEventListener('click', (e) => {
                // 닫기 버튼 누른 경우는 무시
                if ((e.target).classList.contains('tab-close')) return;
                activateRoomFromTab(roomId);
            });

            // 탭 닫기
            tab.querySelector('.tab-close').addEventListener('click', (e) => {
                e.stopPropagation();
                closeRoomTab(roomId);
            });

            tabBarEl.appendChild(tab);
        });
    }

    function activateRoomFromTab(roomId) {
        const room = getRoomById(roomId);
        if (!room) return;
        activeRoomId = roomId;
        pendingFiles = [];
        renderTabBar();
        renderRoomList();
        renderChatArea();
        renderMembers();
    }

    function closeRoomTab(roomId) {
        openRoomTabs = openRoomTabs.filter(id => id !== roomId);

        if (activeRoomId === roomId) {
            // 닫힌 탭이 현재 탭이면, 마지막 탭을 활성화하거나 아무것도 없으면 비우기
            const nextId = openRoomTabs.length ? openRoomTabs[openRoomTabs.length - 1] : null;
            activeRoomId = nextId;
            pendingFiles = [];

            if (nextId) {
                renderRoomList();
                renderChatArea();
                renderMembers();
            } else {
                // 열려있는 방이 없을 때
                chatAreaWrapper.innerHTML = '';
                chatAreaWrapper.appendChild(emptyState);
                emptyState.style.display = 'flex';
                onlineMemberListEl.innerHTML = '';
                offlineMemberListEl.innerHTML = '';
            }
        }

        renderTabBar();
    }

    // -----------------------------
    // 방 목록 렌더링 (READ)
    // -----------------------------
    function renderRoomList() {
        roomListEl.innerHTML = '';

        rooms
            .filter(room =>
                room.name.toLowerCase().includes(roomSearchText.toLowerCase())
            )
            .forEach(room => {
                const lastMsg = room.messages[room.messages.length - 1];
                const lastText = lastMsg ? lastMsg.text : '메시지가 없습니다.';
                const lastTime = lastMsg ? lastMsg.time : '-';
                const memberCount = room.members.length;

                const card = document.createElement('div');
                card.className = 'chat-card';
                if (room.id === activeRoomId) {
                    card.classList.add('active');
                }
                card.dataset.roomId = room.id;

                card.innerHTML = `
                    <div class="chat-header">
                        <span class="room-name">${escapeHtml(room.name)}</span>
                        <span class="time">${lastTime}</span>
                    </div>
                    <div class="chat-body">${escapeHtml(lastText)}</div>
                    <div class="chat-footer">
                        <span class="member-count">${memberCount}명</span>
                        <div class="room-actions">
                            <button class="room-edit">수정</button>
                            <button class="room-delete">삭제</button>
                        </div>
                    </div>
                `;

                // 방 클릭 : 탭 열고 입장
                card.addEventListener('click', () => {
                    openRoom(room.id);
                });

                // 수정
                card.querySelector('.room-edit').addEventListener('click', (e) => {
                    e.stopPropagation();
                    renameRoom(room.id);
                });

                // 삭제
                card.querySelector('.room-delete').addEventListener('click', (e) => {
                    e.stopPropagation();
                    deleteRoom(room.id);
                });

                roomListEl.appendChild(card);
            });

        if (!rooms.length) {
            roomListEl.innerHTML = `<p class="empty-room-list">채팅방이 없습니다. 상단의 [새 채팅방] 버튼을 눌러 만들어 보세요.</p>`;
        }
    }

    // -----------------------------
    // 방 열기 (READ + 탭 관리)
    // -----------------------------
    function openRoom(roomId) {
        const room = getRoomById(roomId);
        if (!room) return;

        // 탭에 없으면 추가
        if (!openRoomTabs.includes(roomId)) {
            openRoomTabs.push(roomId);
        }

        activeRoomId = roomId;
        pendingFiles = [];

        renderTabBar();
        renderRoomList();
        renderChatArea();
        renderMembers();
    }

    // -----------------------------
    // 방 생성 (CREATE)
    // -----------------------------
    function createRoom() {
        const name = prompt('새 채팅방 이름을 입력하세요.');
        if (!name) return;

        const newRoom = {
            id: 'room-' + Date.now(),
            name: name.trim(),
            members: [],
            messages: []
        };
        rooms.push(newRoom);
        renderRoomList();
        openRoom(newRoom.id); // 생성 후 자동으로 탭 + 열기
    }

    // -----------------------------
    // 방 이름 수정 (UPDATE)
    // -----------------------------
    function renameRoom(roomId) {
        const room = getRoomById(roomId);
        if (!room) return;

        const newName = prompt('채팅방 이름을 수정하세요.', room.name);
        if (!newName) return;

        room.name = newName.trim();
        renderRoomList();
        renderTabBar(); // 탭 제목도 반영
        if (roomId === activeRoomId) {
            renderChatArea();
        }
    }

    // -----------------------------
    // 방 삭제 (DELETE)
    // -----------------------------
    function deleteRoom(roomId) {
        const room = getRoomById(roomId);
        if (!room) return;

        if (!confirm(`'${room.name}' 채팅방을 삭제하시겠습니까?\n(메시지와 참여자 정보도 함께 사라집니다.)`)) return;

        rooms = rooms.filter(r => r.id !== roomId);

        // 탭 목록에서도 제거
        openRoomTabs = openRoomTabs.filter(id => id !== roomId);

        if (activeRoomId === roomId) {
            const nextId = openRoomTabs.length ? openRoomTabs[openRoomTabs.length - 1] : null;
            activeRoomId = nextId;
            pendingFiles = [];

            if (nextId) {
                renderRoomList();
                renderChatArea();
                renderMembers();
            } else {
                chatAreaWrapper.innerHTML = '';
                chatAreaWrapper.appendChild(emptyState);
                emptyState.style.display = 'flex';
                onlineMemberListEl.innerHTML = '';
                offlineMemberListEl.innerHTML = '';
            }
        }

        renderRoomList();
        renderTabBar();
    }

    // -----------------------------
    // 중앙: 채팅 영역 렌더링
    // -----------------------------
    function renderChatArea() {
        const room = getRoomById(activeRoomId);
        if (!room) {
            chatAreaWrapper.innerHTML = '';
            chatAreaWrapper.appendChild(emptyState);
            emptyState.style.display = 'flex';
            return;
        }

        emptyState.style.display = 'none';

        chatAreaWrapper.innerHTML = `
            <div class="chat-room-header">
                <div>
                    <div class="chat-room-title">${escapeHtml(room.name)}</div>
                    <div class="chat-room-sub">${room.members.length}명 참여중</div>
                </div>
                <button id="room-rename-btn" class="room-rename-btn">방 이름 변경</button>
            </div>
            <div class="chat-messages" id="chat-messages"></div>
            <div class="chat-input-area" id="chat-input-area">
                <div class="file-preview" id="file-preview"></div>
                <div class="chat-input-row">
                    <button type="button" class="chat-upload-btn" id="chat-upload-btn" title="파일 업로드">file</button>
                    <input type="file" id="chat-file-input" multiple style="display:none">
                    <input type="text" id="chat-input" placeholder="메시지를 입력하세요" autocomplete="off">
                    <button id="chat-send-btn">전송</button>
                </div>
            </div>
        `;

        const messagesEl = document.getElementById('chat-messages');
        renderMessages(room, messagesEl);

        const inputAreaEl = document.getElementById('chat-input-area');
        const previewEl = document.getElementById('file-preview');
        const inputEl = document.getElementById('chat-input');
        const sendBtn = document.getElementById('chat-send-btn');
        const renameBtn = document.getElementById('room-rename-btn');
        const uploadBtn = document.getElementById('chat-upload-btn');
        const fileInput = document.getElementById('chat-file-input');

        // 현재 pendingFiles 기준으로 미리보기 렌더
        renderFilePreview(previewEl);

        sendBtn.addEventListener('click', () => {
            sendMessage();
        });

        inputEl.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.isComposing && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        renameBtn.addEventListener('click', () => {
            renameRoom(activeRoomId);
        });

        // 📎 파일 업로드 버튼 → 숨겨진 file input 클릭
        uploadBtn.addEventListener('click', () => {
            fileInput.click();
        });

        // 파일 선택 → pendingFiles에 쌓고 미리보기
        fileInput.addEventListener('change', () => {
            if (!fileInput.files || !fileInput.files.length) return;
            pendingFiles = pendingFiles.concat(Array.from(fileInput.files));
            fileInput.value = '';
            renderFilePreview(previewEl);
        });

        // 드래그&드롭 (input 영역 위)
        ['dragenter', 'dragover'].forEach(evtName => {
            inputAreaEl.addEventListener(evtName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                inputAreaEl.classList.add('drag-over');
            });
        });

        ['dragleave', 'dragend'].forEach(evtName => {
            inputAreaEl.addEventListener(evtName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                inputAreaEl.classList.remove('drag-over');
            });
        });

        inputAreaEl.addEventListener('drop', (e) => {
            e.preventDefault();
            e.stopPropagation();
            inputAreaEl.classList.remove('drag-over');

            const dt = e.dataTransfer;
            if (!dt || !dt.files || !dt.files.length) return;

            pendingFiles = pendingFiles.concat(Array.from(dt.files));
            renderFilePreview(previewEl);
        });
    }

    // -----------------------------
    // 파일 미리보기 렌더링
    // -----------------------------
    function renderFilePreview(previewEl) {
        if (!previewEl) return;

        if (!pendingFiles.length) {
            previewEl.innerHTML = '';
            previewEl.style.display = 'none';
            return;
        }

        previewEl.style.display = 'flex';
        previewEl.innerHTML = pendingFiles.map((f, idx) => `
            <div class="file-pill" data-index="${idx}">
                <span class="file-name">${escapeHtml(f.name)}</span>
                <button class="file-remove" data-index="${idx}" title="제거">✕</button>
            </div>
        `).join('');

        previewEl.querySelectorAll('.file-remove').forEach(btn => {
            btn.addEventListener('click', () => {
                const index = Number(btn.dataset.index);
                pendingFiles.splice(index, 1);
                renderFilePreview(previewEl);
            });
        });
    }

    // -----------------------------
    // 메시지 목록 렌더링
    // -----------------------------
    function renderMessages(room, messagesEl) {
        messagesEl.innerHTML = '';

        room.messages.forEach(msg => {
            const isMine = msg.sender === MY_NAME;

            const msgDiv = document.createElement('div');
            msgDiv.className = 'message ' + (isMine ? 'mine' : 'other');
            msgDiv.dataset.msgId = msg.id;

            const actionsPart = isMine
                ? `<button class="msg-edit">수정</button><button class="msg-delete">삭제</button>`
                : '';

            msgDiv.innerHTML = `
                <div class="message-body">
                    <div class="bubble">${escapeHtml(msg.text)}</div>
                    <div class="message-meta">
                        <span class="sender">${escapeHtml(msg.sender)}</span>
                        <span class="timestamp">${msg.time}</span>
                        ${actionsPart}
                    </div>
                </div>
            `;

            // 내 메시지일 때만 수정/삭제 이벤트 연결
            if (isMine) {
                msgDiv.querySelector('.msg-edit').addEventListener('click', () => {
                    editMessage(room.id, msg.id);
                });

                msgDiv.querySelector('.msg-delete').addEventListener('click', () => {
                    deleteMessage(room.id, msg.id);
                });
            }

            messagesEl.appendChild(msgDiv);
        });

        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    // -----------------------------
    // 메시지 전송 (CREATE)
    // -----------------------------
    function sendMessage() {
        const room = getRoomById(activeRoomId);
        if (!room) return;

        const inputEl = document.getElementById('chat-input');
        const previewEl = document.getElementById('file-preview');
        if (!inputEl) return;

        const text = (inputEl.value || '').trim();
        const hasFiles = pendingFiles.length > 0;

        if (!text && !hasFiles) return;

        let msgText = text;

        if (hasFiles) {
            const fileNames = pendingFiles.map(f => f.name).join(', ');
            const filePart = `[파일] ${fileNames}`;
            msgText = msgText ? `${msgText}\n${filePart}` : filePart;
        }

        const newMsg = {
            id: 'msg-' + Date.now(),
            sender: MY_NAME,
            text: msgText,
            time: getCurrentTime()
        };

        room.messages.push(newMsg);
        inputEl.value = '';

        // 파일 미리보기 초기화
        pendingFiles = [];
        renderFilePreview(previewEl);

        const messagesEl = document.getElementById('chat-messages');
        renderMessages(room, messagesEl);
        renderRoomList(); // 최근 메시지/시간 갱신
    }

    // -----------------------------
    // 메시지 수정 (UPDATE) - 내 것만
    // -----------------------------
    function editMessage(roomId, msgId) {
        const room = getRoomById(roomId);
        if (!room) return;
        const msg = room.messages.find(m => m.id === msgId);
        if (!msg) return;
        if (msg.sender !== MY_NAME) {
            alert('상대방 메시지는 수정할 수 없습니다.');
            return;
        }

        const newText = prompt('메시지를 수정하세요.', msg.text);
        if (!newText) return;

        msg.text = newText.trim();
        const messagesEl = document.getElementById('chat-messages');
        renderMessages(room, messagesEl);
        renderRoomList();
    }

    // -----------------------------
    // 메시지 삭제 (DELETE) - 내 것만
    // -----------------------------
    function deleteMessage(roomId, msgId) {
        const room = getRoomById(roomId);
        if (!room) return;
        const msg = room.messages.find(m => m.id === msgId);
        if (!msg) return;
        if (msg.sender !== MY_NAME) {
            alert('상대방 메시지는 삭제할 수 없습니다.');
            return;
        }

        if (!confirm('이 메시지를 삭제하시겠습니까?')) return;

        room.messages = room.messages.filter(m => m.id !== msgId);
        const messagesEl = document.getElementById('chat-messages');
        renderMessages(room, messagesEl);
        renderRoomList();
    }

    // -----------------------------
    // 참여자 목록 렌더링 (READ)
    // -----------------------------
    function renderMembers() {
        const room = getRoomById(activeRoomId);
        if (!room) {
            onlineMemberListEl.innerHTML = '';
            offlineMemberListEl.innerHTML = '';
            return;
        }

        const filtered = room.members.filter(m =>
            (m.name + m.role).toLowerCase().includes(memberSearchText.toLowerCase())
        );

        const online = filtered.filter(m => m.status === 'online' || m.status === 'busy');
        const offline = filtered.filter(m => m.status === 'offline');

        onlineMemberListEl.innerHTML = '';
        offlineMemberListEl.innerHTML = '';

        online.forEach(m => {
            onlineMemberListEl.appendChild(createMemberRow(room.id, m));
        });

        offline.forEach(m => {
            offlineMemberListEl.appendChild(createMemberRow(room.id, m));
        });
    }

    // 참여자 행 생성 (수정 X, 삭제만)
    function createMemberRow(roomId, member) {
        const wrapper = document.createElement('div');
        wrapper.className = 'member';
        wrapper.dataset.memberId = member.id;

        let statusClass = 'online';
        let statusText = '온라인';
        let dotClass = 'green';

        if (member.status === 'busy') {
            statusClass = 'busy';
            statusText = '다른 용무중';
            dotClass = 'orange';
        } else if (member.status === 'offline') {
            statusClass = 'offline';
            statusText = '오프라인';
            dotClass = 'red';
        }

        wrapper.innerHTML = `
            <div class="profile">
                <img src="https://via.placeholder.com/40" alt="${escapeHtml(member.name)}">
                <div class="info">
                    <span class="name">${escapeHtml(member.name)}</span>
                    <span class="role">${escapeHtml(member.role)}</span>
                </div>
            </div>
            <div class="status ${statusClass}">
                <span class="dot ${dotClass}"></span>${statusText}
                <button class="member-remove">삭제</button>
            </div>
        `;

        wrapper.querySelector('.member-remove').addEventListener('click', () => {
            removeMember(roomId, member.id);
        });

        return wrapper;
    }

    // -----------------------------
    // 참여자 추가 (CREATE)
    // -----------------------------
    function addMember() {
        const room = getRoomById(activeRoomId);
        if (!room) {
            alert('먼저 채팅방을 선택해주세요.');
            return;
        }

        const name = prompt('참여자 이름을 입력하세요.');
        if (!name) return;
        const role = prompt('역할/직책을 입력하세요. (예: 개발자, 디자이너)');
        if (!role) return;

        const newMember = {
            id: 'mem-' + Date.now(),
            name: name.trim(),
            role: role.trim(),
            status: 'online'
        };

        room.members.push(newMember);
        renderMembers();
        renderRoomList();
    }

    // -----------------------------
    // 참여자 삭제 (DELETE)
    // -----------------------------
    function removeMember(roomId, memberId) {
        const room = getRoomById(roomId);
        if (!room) return;
        const member = room.members.find(m => m.id === memberId);
        if (!member) return;

        if (!confirm(`'${member.name}' 참여자를 제거하시겠습니까?`)) return;

        room.members = room.members.filter(m => m.id !== memberId);
        renderMembers();
        renderRoomList();
    }

    // -----------------------------
    // 검색 이벤트
    // -----------------------------
    roomSearchInput.addEventListener('input', () => {
        roomSearchText = roomSearchInput.value || '';
        renderRoomList();
    });

    memberSearchInput.addEventListener('input', () => {
        memberSearchText = memberSearchInput.value || '';
        renderMembers();
    });

    // -----------------------------
    // 버튼 이벤트 연결
    // -----------------------------
    newRoomBtn.addEventListener('click', createRoom);
    addMemberBtn.addEventListener('click', addMember);

    // -----------------------------
    // 초기 렌더링
    // -----------------------------
    renderRoomList();
    if (rooms.length) {
        openRoom(rooms[0].id); // 첫 번째 방 탭 + 열기
    }
});