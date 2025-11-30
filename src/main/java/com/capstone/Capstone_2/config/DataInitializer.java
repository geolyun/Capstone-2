package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.entity.Category;
import com.capstone.Capstone_2.entity.Region;
import com.capstone.Capstone_2.repository.CategoryRepository;
import com.capstone.Capstone_2.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            // 대주제만 있고 소주제가 없으면 선택이 불가능하므로, '전체'라는 기본 소주제를 하나씩 넣어줍니다.
            createCategoryGroup("공강시간", List.of("전체"));
            createCategoryGroup("약속모임", List.of("전체"));
            createCategoryGroup("카공", List.of("전체"));
            createCategoryGroup("데이트", List.of("전체"));
        }

        if (regionRepository.count() == 0) {
            initRegionData();
        }
    }

    private void createCategoryGroup(String rootName, List<String> childrenNames) {
        // 대주제(Root) 생성
        Category root = Category.builder().name(rootName).slug(rootName).build();
        categoryRepository.save(root);

        // 소주제(Children) 생성
        for (String childName : childrenNames) {
            Category child = Category.builder()
                    .name(childName)
                    .slug(rootName + "-" + childName) // slug 중복 방지를 위해 prefix 추가 권장
                    .parent(root)
                    .build();
            categoryRepository.save(child);
        }
    }

    private void initRegionData() {
        // ==========================================
        // 1. 서울특별시 (11)
        // ==========================================
        Region seoul = createRegion("서울", "11", null);
        createRegion("종로구", "11110", seoul);
        createRegion("중구", "11140", seoul);
        createRegion("용산구", "11170", seoul);
        createRegion("성동구", "11200", seoul);
        createRegion("광진구", "11215", seoul);
        createRegion("동대문구", "11230", seoul);
        createRegion("중랑구", "11260", seoul);
        createRegion("성북구", "11290", seoul);
        createRegion("강북구", "11305", seoul);
        createRegion("도봉구", "11320", seoul);
        createRegion("노원구", "11350", seoul);
        createRegion("은평구", "11380", seoul);
        createRegion("서대문구", "11410", seoul);
        createRegion("마포구", "11440", seoul);
        createRegion("양천구", "11470", seoul);
        createRegion("강서구", "11500", seoul);
        createRegion("구로구", "11530", seoul);
        createRegion("금천구", "11545", seoul);
        createRegion("영등포구", "11560", seoul);
        createRegion("동작구", "11590", seoul);
        createRegion("관악구", "11620", seoul);
        createRegion("서초구", "11650", seoul);
        createRegion("강남구", "11680", seoul); //
        createRegion("송파구", "11710", seoul);
        createRegion("강동구", "11740", seoul);

        // ==========================================
        // 2. 부산광역시 (26)
        // ==========================================
        Region busan = createRegion("부산", "26", null);
        createRegion("중구", "26110", busan);
        createRegion("서구", "26140", busan);
        createRegion("동구", "26170", busan);
        createRegion("영도구", "26200", busan);
        createRegion("부산진구", "26230", busan);
        createRegion("동래구", "26260", busan);
        createRegion("남구", "26290", busan);
        createRegion("북구", "26320", busan);
        createRegion("해운대구", "26350", busan);
        createRegion("사하구", "26380", busan);
        createRegion("금정구", "26410", busan);
        createRegion("강서구", "26440", busan);
        createRegion("연제구", "26470", busan);
        createRegion("수영구", "26500", busan);
        createRegion("사상구", "26530", busan);
        createRegion("기장군", "26710", busan);

        // ==========================================
        // 3. 대구광역시 (27)
        // ==========================================
        Region daegu = createRegion("대구", "27", null);
        createRegion("중구", "27110", daegu);
        createRegion("동구", "27140", daegu);
        createRegion("서구", "27170", daegu);
        createRegion("남구", "27200", daegu);
        createRegion("북구", "27230", daegu);
        createRegion("수성구", "27260", daegu);
        createRegion("달서구", "27290", daegu);
        createRegion("달성군", "27710", daegu);
        createRegion("군위군", "27720", daegu); // 2023년 편입

        // ==========================================
        // 4. 인천광역시 (28)
        // ==========================================
        Region incheon = createRegion("인천", "28", null);
        createRegion("중구", "28110", incheon);
        createRegion("동구", "28140", incheon);
        createRegion("미추홀구", "28177", incheon);
        createRegion("연수구", "28185", incheon);
        createRegion("남동구", "28200", incheon);
        createRegion("부평구", "28237", incheon);
        createRegion("계양구", "28245", incheon);
        createRegion("서구", "28260", incheon);
        createRegion("강화군", "28710", incheon);
        createRegion("옹진군", "28720", incheon);

        // ==========================================
        // 5. 광주광역시 (29)
        // ==========================================
        Region gwangju = createRegion("광주", "29", null);
        createRegion("동구", "29110", gwangju);
        createRegion("서구", "29140", gwangju);
        createRegion("남구", "29155", gwangju);
        createRegion("북구", "29170", gwangju);
        createRegion("광산구", "29200", gwangju);

        // ==========================================
        // 6. 대전광역시 (30)
        // ==========================================
        Region daejeon = createRegion("대전", "30", null);
        createRegion("동구", "30110", daejeon);
        createRegion("중구", "30140", daejeon);
        createRegion("서구", "30170", daejeon);
        createRegion("유성구", "30200", daejeon);
        createRegion("대덕구", "30230", daejeon);

        // ==========================================
        // 7. 울산광역시 (31)
        // ==========================================
        Region ulsan = createRegion("울산", "31", null);
        createRegion("중구", "31110", ulsan);
        createRegion("남구", "31140", ulsan);
        createRegion("동구", "31170", ulsan);
        createRegion("북구", "31200", ulsan);
        createRegion("울주군", "31710", ulsan);

        // ==========================================
        // 8. 세종특별자치시 (36)
        // ==========================================
        createRegion("세종", "36", null); // 하위 행정구역 없음

        // ==========================================
        // 9. 경기도 (41)
        // ==========================================
        Region gyeonggi = createRegion("경기", "41", null);
        createRegion("수원시 장안구", "41111", gyeonggi);
        createRegion("수원시 권선구", "41113", gyeonggi);
        createRegion("수원시 팔달구", "41115", gyeonggi);
        createRegion("수원시 영통구", "41117", gyeonggi);
        createRegion("성남시 수정구", "41131", gyeonggi);
        createRegion("성남시 중원구", "41133", gyeonggi);
        createRegion("성남시 분당구", "41135", gyeonggi);
        createRegion("의정부시", "41150", gyeonggi);
        createRegion("안양시 만안구", "41171", gyeonggi);
        createRegion("안양시 동안구", "41173", gyeonggi);
        createRegion("부천시", "41190", gyeonggi);
        createRegion("광명시", "41210", gyeonggi);
        createRegion("평택시", "41220", gyeonggi);
        createRegion("동두천시", "41250", gyeonggi);
        createRegion("안산시 상록구", "41271", gyeonggi);
        createRegion("안산시 단원구", "41273", gyeonggi);
        createRegion("고양시 덕양구", "41281", gyeonggi);
        createRegion("고양시 일산동구", "41285", gyeonggi);
        createRegion("고양시 일산서구", "41287", gyeonggi);
        createRegion("과천시", "41290", gyeonggi);
        createRegion("구리시", "41310", gyeonggi);
        createRegion("남양주시", "41360", gyeonggi);
        createRegion("오산시", "41370", gyeonggi);
        createRegion("시흥시", "41390", gyeonggi);
        createRegion("군포시", "41410", gyeonggi);
        createRegion("의왕시", "41430", gyeonggi);
        createRegion("하남시", "41450", gyeonggi);
        createRegion("용인시 처인구", "41461", gyeonggi);
        createRegion("용인시 기흥구", "41463", gyeonggi);
        createRegion("용인시 수지구", "41465", gyeonggi);
        createRegion("파주시", "41480", gyeonggi);
        createRegion("이천시", "41500", gyeonggi);
        createRegion("안성시", "41550", gyeonggi);
        createRegion("김포시", "41570", gyeonggi);
        createRegion("화성시", "41590", gyeonggi);
        createRegion("광주시", "41610", gyeonggi);
        createRegion("양주시", "41630", gyeonggi);
        createRegion("포천시", "41650", gyeonggi);
        createRegion("여주시", "41670", gyeonggi);
        createRegion("연천군", "41800", gyeonggi);
        createRegion("가평군", "41820", gyeonggi);
        createRegion("양평군", "41830", gyeonggi);

        // ==========================================
        // 10. 강원특별자치도 (51) - (구 42)
        // ==========================================
        Region gangwon = createRegion("강원", "51", null);
        createRegion("춘천시", "51110", gangwon);
        createRegion("원주시", "51130", gangwon);
        createRegion("강릉시", "51150", gangwon);
        createRegion("동해시", "51170", gangwon);
        createRegion("태백시", "51190", gangwon);
        createRegion("속초시", "51210", gangwon);
        createRegion("삼척시", "51230", gangwon);
        createRegion("홍천군", "51720", gangwon);
        createRegion("횡성군", "51730", gangwon);
        createRegion("영월군", "51750", gangwon);
        createRegion("평창군", "51760", gangwon);
        createRegion("정선군", "51770", gangwon);
        createRegion("철원군", "51780", gangwon);
        createRegion("화천군", "51790", gangwon);
        createRegion("양구군", "51800", gangwon);
        createRegion("인제군", "51810", gangwon);
        createRegion("고성군", "51820", gangwon);
        createRegion("양양군", "51830", gangwon);

        // ==========================================
        // 11. 충청북도 (43)
        // ==========================================
        Region chungbuk = createRegion("충북", "43", null);
        createRegion("청주시 상당구", "43111", chungbuk);
        createRegion("청주시 서원구", "43112", chungbuk);
        createRegion("청주시 흥덕구", "43113", chungbuk);
        createRegion("청주시 청원구", "43114", chungbuk);
        createRegion("충주시", "43130", chungbuk);
        createRegion("제천시", "43150", chungbuk);
        createRegion("보은군", "43720", chungbuk);
        createRegion("옥천군", "43730", chungbuk);
        createRegion("영동군", "43740", chungbuk);
        createRegion("증평군", "43745", chungbuk);
        createRegion("진천군", "43750", chungbuk);
        createRegion("괴산군", "43760", chungbuk);
        createRegion("음성군", "43770", chungbuk);
        createRegion("단양군", "43800", chungbuk);

        // ==========================================
        // 12. 충청남도 (44)
        // ==========================================
        Region chungnam = createRegion("충남", "44", null);
        createRegion("천안시 동남구", "44131", chungnam);
        createRegion("천안시 서북구", "44133", chungnam);
        createRegion("공주시", "44150", chungnam);
        createRegion("보령시", "44180", chungnam);
        createRegion("아산시", "44200", chungnam);
        createRegion("서산시", "44210", chungnam);
        createRegion("논산시", "44230", chungnam);
        createRegion("계룡시", "44250", chungnam);
        createRegion("당진시", "44270", chungnam);
        createRegion("금산군", "44710", chungnam);
        createRegion("부여군", "44760", chungnam);
        createRegion("서천군", "44770", chungnam);
        createRegion("청양군", "44790", chungnam);
        createRegion("홍성군", "44800", chungnam);
        createRegion("예산군", "44810", chungnam);
        createRegion("태안군", "44825", chungnam);

        // ==========================================
        // 13. 전북특별자치도 (52) - (구 45)
        // ==========================================
        Region jeonbuk = createRegion("전북", "52", null);
        createRegion("전주시 완산구", "52111", jeonbuk);
        createRegion("전주시 덕진구", "52113", jeonbuk);
        createRegion("군산시", "52130", jeonbuk);
        createRegion("익산시", "52140", jeonbuk);
        createRegion("정읍시", "52180", jeonbuk);
        createRegion("남원시", "52190", jeonbuk);
        createRegion("김제시", "52210", jeonbuk);
        createRegion("완주군", "52710", jeonbuk);
        createRegion("진안군", "52720", jeonbuk);
        createRegion("무주군", "52730", jeonbuk);
        createRegion("장수군", "52740", jeonbuk);
        createRegion("임실군", "52750", jeonbuk);
        createRegion("순창군", "52770", jeonbuk);
        createRegion("고창군", "52790", jeonbuk);
        createRegion("부안군", "52800", jeonbuk);

        // ==========================================
        // 14. 전라남도 (46)
        // ==========================================
        Region jeonnam = createRegion("전남", "46", null);
        createRegion("목포시", "46110", jeonnam);
        createRegion("여수시", "46130", jeonnam);
        createRegion("순천시", "46150", jeonnam);
        createRegion("나주시", "46170", jeonnam);
        createRegion("광양시", "46230", jeonnam);
        createRegion("담양군", "46710", jeonnam);
        createRegion("곡성군", "46720", jeonnam);
        createRegion("구례군", "46730", jeonnam);
        createRegion("고흥군", "46770", jeonnam);
        createRegion("보성군", "46780", jeonnam);
        createRegion("화순군", "46790", jeonnam);
        createRegion("장흥군", "46800", jeonnam);
        createRegion("강진군", "46810", jeonnam);
        createRegion("해남군", "46820", jeonnam);
        createRegion("영암군", "46830", jeonnam);
        createRegion("무안군", "46840", jeonnam);
        createRegion("함평군", "46860", jeonnam);
        createRegion("영광군", "46870", jeonnam);
        createRegion("장성군", "46880", jeonnam);
        createRegion("완도군", "46890", jeonnam);
        createRegion("진도군", "46900", jeonnam);
        createRegion("신안군", "46910", jeonnam);

        // ==========================================
        // 15. 경상북도 (47)
        // ==========================================
        Region gyeongbuk = createRegion("경북", "47", null);
        createRegion("포항시 남구", "47111", gyeongbuk);
        createRegion("포항시 북구", "47113", gyeongbuk);
        createRegion("경주시", "47130", gyeongbuk);
        createRegion("김천시", "47150", gyeongbuk);
        createRegion("안동시", "47170", gyeongbuk);
        createRegion("구미시", "47190", gyeongbuk);
        createRegion("영주시", "47210", gyeongbuk);
        createRegion("영천시", "47230", gyeongbuk);
        createRegion("상주시", "47250", gyeongbuk);
        createRegion("문경시", "47280", gyeongbuk);
        createRegion("경산시", "47290", gyeongbuk);
        createRegion("의성군", "47730", gyeongbuk);
        createRegion("청송군", "47750", gyeongbuk);
        createRegion("영양군", "47760", gyeongbuk);
        createRegion("영덕군", "47770", gyeongbuk);
        createRegion("청도군", "47820", gyeongbuk);
        createRegion("고령군", "47830", gyeongbuk);
        createRegion("성주군", "47840", gyeongbuk);
        createRegion("칠곡군", "47850", gyeongbuk);
        createRegion("예천군", "47900", gyeongbuk);
        createRegion("봉화군", "47920", gyeongbuk);
        createRegion("울진군", "47930", gyeongbuk);
        createRegion("울릉군", "47940", gyeongbuk);

        // ==========================================
        // 16. 경상남도 (48)
        // ==========================================
        Region gyeongnam = createRegion("경남", "48", null);
        createRegion("창원시 의창구", "48121", gyeongnam);
        createRegion("창원시 성산구", "48123", gyeongnam);
        createRegion("창원시 마산합포구", "48125", gyeongnam);
        createRegion("창원시 마산회원구", "48127", gyeongnam);
        createRegion("창원시 진해구", "48129", gyeongnam);
        createRegion("진주시", "48170", gyeongnam);
        createRegion("통영시", "48220", gyeongnam);
        createRegion("사천시", "48240", gyeongnam);
        createRegion("김해시", "48250", gyeongnam);
        createRegion("밀양시", "48270", gyeongnam);
        createRegion("거제시", "48310", gyeongnam);
        createRegion("양산시", "48330", gyeongnam);
        createRegion("의령군", "48720", gyeongnam);
        createRegion("함안군", "48730", gyeongnam);
        createRegion("창녕군", "48740", gyeongnam);
        createRegion("고성군", "48820", gyeongnam);
        createRegion("남해군", "48840", gyeongnam);
        createRegion("하동군", "48850", gyeongnam);
        createRegion("산청군", "48860", gyeongnam);
        createRegion("함양군", "48870", gyeongnam);
        createRegion("거창군", "48880", gyeongnam);
        createRegion("합천군", "48890", gyeongnam);

        // ==========================================
        // 17. 제주특별자치도 (50)
        // ==========================================
        Region jeju = createRegion("제주", "50", null);
        createRegion("제주시", "50110", jeju);
        createRegion("서귀포시", "50130", jeju);
    }

    private Region createRegion(String name, String code, Region parent) {
        Region region = Region.builder()
                .name(name)
                .code(code)
                .parent(parent)
                .build();
        return regionRepository.save(region);
    }
}