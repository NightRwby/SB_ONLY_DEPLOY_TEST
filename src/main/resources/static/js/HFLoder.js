$(document).ready(function() {
    const headerPath = "/header.html";
    const footerPath = "/footer.html";
    // 로드 실행
    $("#header-placeholder").load(headerPath);
    $("#footer-placeholder").load(footerPath);
});
