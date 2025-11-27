package com.example.demo.util;

public class KoreanNameUtil {

    // 초성(Choseong) 목록: 'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    private static final char[] CHOSEONG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    /**
     * 입력된 문자열에서 한글 초성만을 추출하여 반환합니다.
     * 한글이 아닌 문자(영어, 숫자 등)는 그대로 유지합니다.
     * @param name 초성을 추출할 사용자 이름 (예: "홍길동")
     * @return 추출된 초성 문자열 (예: "ㅎㄱㄷ")
     */
    public static String extractInitial(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);

            // 1. 해당 문자가 한글인지 확인 (가: 44032, 힣: 55203)
            if (ch >= 0xAC00 && ch <= 0xD7A3) { // 0xAC00 (가) ~ 0xD7A3 (힣)

                // 2. 한글 유니코드 계산
                // '가' (0xAC00)를 뺀 후, 한글 자모가 가지는 값으로 나눕니다.
                int uniVal = ch - 0xAC00;

                // 3. 초성 인덱스 계산
                // 초성 = (유니코드 값) / (중성 개수 * 종성 개수)
                // 중성 개수 (21) * 종성 개수 (28) = 588
                int choseongIndex = uniVal / 588;

                // 4. 초성 배열에서 초성 문자 추가
                result.append(CHOSEONG[choseongIndex]);
            } else {
                // 한글이 아닌 문자는 그대로 추가 (예: 영어, 숫자, 공백)
                result.append(ch);
            }
        }
        return result.toString();
    }
}