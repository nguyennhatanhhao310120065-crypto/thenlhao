# BÁO CÁO ĐỒ ÁN PBL6 – CÔNG NGHỆ PHẦN MỀM

**Đề tài: XÂY DỰNG HỆ THỐNG NGÂN HÀNG CÂU HỎI TỪ FILE NGHE TIẾNG ANH**

> *Ghi chú định dạng cho Word: Font **Times New Roman**, cỡ chữ **13**, giãn dòng **1.3**, các đoạn văn **căn đều hai bên (Justify)**. Tiêu đề chương để cỡ 14–16, in đậm. Đánh số tự động theo cấu trúc 1., 1.1., 1.1.1.*

---

**TRƯỜNG ĐẠI HỌC BÁCH KHOA – ĐẠI HỌC ĐÀ NẴNG**
**KHOA CÔNG NGHỆ THÔNG TIN**

**Học phần:** PBL6 – Công nghệ phần mềm
**Tên đề tài:** Xây dựng hệ thống ngân hàng câu hỏi từ file nghe tiếng Anh

**Nhóm thực hiện – Nhóm 24Nh10, lớp 24T_DT1:**

| MSSV | Họ và tên |
|------|-----------|
| 102240024 | Nguyễn Nhật Anh Hào |
| 102240052 | Nguyễn Tấn Quốc |
| 102240025 | Phạm Ngọc Hiệp |

**Giảng viên hướng dẫn:** Phạm Minh Tuấn
**Thời gian thực hiện:** Tháng 6 năm 2026

---

## MỞ ĐẦU

### 1. Lý do chọn đề tài

Trong bối cảnh hội nhập quốc tế, năng lực tiếng Anh – đặc biệt là kỹ năng Nghe (Listening) – ngày càng trở thành yêu cầu thiết yếu đối với sinh viên và người đi làm. Các kỳ thi chuẩn hóa như IELTS, TOEIC, VSTEP đều dành một tỷ trọng điểm số lớn cho phần Nghe. Tuy nhiên, thực tiễn dạy và học cho thấy việc luyện nghe hiệu quả đòi hỏi một nguồn câu hỏi phong phú, đa dạng về dạng thức và bám sát nội dung audio. Việc biên soạn thủ công các bộ câu hỏi này là một công việc tốn nhiều thời gian, công sức: giảng viên phải nghe đi nghe lại đoạn ghi âm, gỡ băng (transcript), rồi tự đặt câu hỏi, soạn đáp án và giải thích cho từng câu. Với khối lượng học liệu ngày một lớn, cách làm thủ công không còn đáp ứng kịp nhu cầu.

Sự phát triển mạnh mẽ của các mô hình ngôn ngữ lớn (Large Language Models) và đặc biệt là các mô hình đa phương thức (multimodal) như Google Gemini đã mở ra khả năng tự động hóa toàn bộ quy trình trên. Một mô hình đa phương thức có thể tiếp nhận trực tiếp tệp âm thanh, gỡ băng nội dung và sinh ra câu hỏi kèm đáp án một cách nhanh chóng. Xuất phát từ nhu cầu thực tế đó, nhóm chọn đề tài *"Xây dựng hệ thống ngân hàng câu hỏi từ file nghe tiếng Anh"* nhằm ứng dụng trí tuệ nhân tạo để hỗ trợ giảng viên và người học tạo lập, quản lý và luyện tập với ngân hàng câu hỏi nghe một cách hiệu quả.

### 2. Mục tiêu

Mục tiêu tổng quát của đồ án là xây dựng một ứng dụng desktop cho phép tự động sinh ngân hàng câu hỏi trắc nghiệm tiếng Anh từ tệp âm thanh đầu vào, dựa trên Google Gemini API. Các mục tiêu cụ thể bao gồm:

- Cho phép người dùng nạp tệp âm thanh (MP3, WAV, M4A, OGG…) và tự động gỡ băng nội dung thành transcript.
- Sinh tự động nhiều dạng câu hỏi (trắc nghiệm, đúng/sai, điền khuyết, nối, hoàn thành câu…) kèm đáp án và giải thích.
- Quản lý ngân hàng câu hỏi, lưu trữ bền vững và cho phép tra cứu lại lịch sử câu hỏi đã tạo.
- Cung cấp môi trường làm bài thi trực tuyến có chấm điểm, đồng bộ audio – transcript và bảng xếp hạng.
- Phân quyền giảng viên/sinh viên, hỗ trợ chia sẻ đề công khai trong thư viện học liệu.

### 3. Phạm vi và đối tượng nghiên cứu

**Đối tượng nghiên cứu:** Quy trình tự động sinh câu hỏi luyện nghe tiếng Anh từ dữ liệu âm thanh, kỹ thuật tích hợp mô hình AI đa phương thức (Google Gemini) vào ứng dụng Java, và mô hình lưu trữ – quản lý ngân hàng câu hỏi.

**Phạm vi:** Đồ án tập trung vào ứng dụng desktop chạy trên máy đơn, ngôn ngữ giao diện là các đề tiếng Anh, sử dụng cơ sở dữ liệu cục bộ SQLite. Hệ thống hỗ trợ các định dạng âm thanh phổ biến và các dạng câu hỏi tiêu chuẩn của IELTS/TOEIC/VSTEP. Đồ án không bao gồm triển khai web quy mô lớn hay huấn luyện mô hình AI riêng, mà khai thác năng lực của mô hình Gemini có sẵn thông qua API.

### 4. Phương pháp thực hiện

Nhóm kết hợp nhiều phương pháp: (i) *nghiên cứu lý thuyết* về mô hình ngôn ngữ lớn đa phương thức, kỹ thuật prompt engineering và xử lý âm thanh trong Java; (ii) *phân tích – thiết kế hệ thống* theo hướng phân tách trách nhiệm (giao diện, dịch vụ AI, truy xuất dữ liệu); (iii) *lập trình thực nghiệm* trên nền tảng JavaFX và thư viện chuẩn `java.net.http`; (iv) *kiểm thử* theo các kịch bản đầu vào hợp lệ và bất thường để đánh giá độ ổn định.

### 5. Cấu trúc báo cáo

Báo cáo gồm năm chương. Chương 1 trình bày tổng quan và tính cấp thiết của đề tài. Chương 2 trình bày ý tưởng và cơ sở lý thuyết nền tảng. Chương 3 phát biểu bài toán và mô tả thuật toán triển khai. Chương 4 trình bày tổ chức chương trình, cách ứng dụng AI, kiểm thử và kết quả thực nghiệm. Chương 5 kết luận và đề xuất hướng phát triển. Cuối báo cáo là tài liệu tham khảo và phụ lục mã nguồn.

---

## CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI

### 1.1. Bối cảnh thực tế

Kỹ năng nghe hiểu là một trong những kỹ năng khó rèn luyện nhất đối với người học tiếng Anh, bởi nó đòi hỏi khả năng xử lý thông tin theo thời gian thực, nhận diện ngữ âm, từ vựng và ngữ cảnh đồng thời. Để luyện tập hiệu quả, người học cần được tiếp xúc với khối lượng lớn các đoạn audio đa dạng kèm theo hệ thống câu hỏi kiểm tra mức độ hiểu. Trên thực tế, các trung tâm ngoại ngữ, giảng viên và cả người tự học đều gặp một điểm nghẽn chung: nguồn câu hỏi luyện nghe có chất lượng luôn khan hiếm và tốn kém để sản xuất.

Quy trình biên soạn câu hỏi truyền thống thường gồm các bước: tìm/ghi âm đoạn audio, nghe và gỡ băng thủ công, xác định các "điểm thông tin" có thể ra đề, soạn câu hỏi theo từng dạng, viết đáp án và lời giải thích. Một đoạn audio dài vài phút có thể tiêu tốn hàng giờ biên soạn. Hơn nữa, chất lượng câu hỏi phụ thuộc nhiều vào kinh nghiệm cá nhân, dẫn đến sự thiếu đồng đều.

### 1.2. Vấn đề đặt ra và giải pháp đề xuất

Bài toán đặt ra là: *làm thế nào để tự động hóa quá trình tạo câu hỏi luyện nghe, vừa nhanh, vừa đảm bảo chất lượng và đa dạng dạng thức?* Trước đây, để giải quyết bài toán này bằng máy tính, người ta phải ghép nối nhiều công nghệ rời rạc: một hệ thống nhận dạng tiếng nói (ASR) để gỡ băng, một mô-đun phân tích ngữ nghĩa để chọn điểm ra đề, và một bộ luật/khuôn mẫu để sinh câu hỏi – mỗi khâu đều phức tạp và dễ sai.

Sự ra đời của các mô hình AI đa phương thức như Google Gemini cho phép gộp các khâu này lại. Mô hình có thể nhận trực tiếp tệp âm thanh, "hiểu" nội dung, gỡ băng và sinh câu hỏi trong một quy trình thống nhất, được điều khiển bằng các chỉ dẫn (prompt) ngôn ngữ tự nhiên. Giải pháp của nhóm là xây dựng một ứng dụng desktop đóng vai trò "nhạc trưởng": cung cấp giao diện thân thiện để người dùng nạp audio, cấu hình loại câu hỏi mong muốn, gọi Gemini API để sinh đề, sau đó lưu trữ và tổ chức kết quả thành một ngân hàng câu hỏi có thể tái sử dụng, làm bài và theo dõi tiến bộ.

### 1.3. Tính cấp thiết và ý nghĩa thực tiễn

Đề tài có tính cấp thiết cao bởi nó trực tiếp giải quyết một nhu cầu phổ biến trong giáo dục ngoại ngữ. Về mặt thực tiễn, hệ thống mang lại nhiều giá trị: **rút ngắn đáng kể thời gian** soạn đề (từ hàng giờ xuống vài chục giây); **chuẩn hóa và đa dạng hóa** dạng câu hỏi theo từng kỳ thi; **giảm rào cản** cho người tự học khi họ có thể tự biến bất kỳ đoạn audio yêu thích nào thành một bài luyện tập hoàn chỉnh; và **tạo môi trường luyện tập khép kín** với chấm điểm, giải thích và bảng xếp hạng nhằm tăng động lực học tập.

Về mặt học thuật, đề tài là một trường hợp điển hình minh họa cách tích hợp một dịch vụ AI hiện đại vào một ứng dụng phần mềm truyền thống bằng Java thuần, qua đó củng cố các kiến thức về kiến trúc phần mềm, lập trình hướng đối tượng, lập trình mạng (HTTP), xử lý JSON, đa luồng (multithreading) và thiết kế giao diện người dùng.

---

## CHƯƠNG 2: CƠ SỞ LÝ THUYẾT

### 2.1. Ý tưởng

Ý tưởng cốt lõi của hệ thống là biến một **tệp âm thanh tiếng Anh** bất kỳ thành một **bộ câu hỏi luyện nghe hoàn chỉnh** một cách tự động. Luồng ý tưởng diễn ra như sau: người dùng cung cấp một tệp audio (ví dụ một đoạn hội thoại, một bài giảng, một bản tin); hệ thống gửi tệp này lên mô hình AI đa phương thức để **gỡ băng** thành transcript có dấu thời gian và phân chia theo các phần (Part); tiếp đó, người dùng lựa chọn các dạng câu hỏi và số lượng mong muốn (hoặc ra lệnh bằng ngôn ngữ tự nhiên cho một "AI Agent"); hệ thống xây dựng một **prompt** giàu ngữ cảnh và gửi tới Gemini để **sinh câu hỏi** kèm đáp án, đoạn trích dẫn minh chứng (transcript quote) và lời giải thích; cuối cùng, kết quả được hiển thị để người dùng rà soát và **lưu vào ngân hàng câu hỏi**.

Điểm đặc sắc của ý tưởng là tính "đa phương thức – một lượt": thay vì tách rời nhận dạng tiếng nói và sinh câu hỏi, hệ thống tận dụng năng lực hiểu cả âm thanh lẫn văn bản của Gemini để liên kết câu hỏi với đúng đoạn audio sinh ra nó, nhờ đó người học có thể nghe lại chính xác đoạn liên quan đến từng câu hỏi.

### 2.2. Cơ sở lý thuyết

**2.2.1. Xử lý tệp âm thanh trong Java**

Java cung cấp các cơ chế làm việc với âm thanh. Trong đồ án, việc **phát lại** âm thanh được thực hiện qua thư viện **JavaFX Media** (`javafx.scene.media.Media` và `MediaPlayer`), hỗ trợ phát, tạm dừng, tua (seek) và lắng nghe sự thay đổi thời gian phát (`currentTimeProperty`). Cơ chế lắng nghe thời gian này là nền tảng để **đồng bộ transcript với audio**: khi audio chạy đến mốc thời gian nào, dòng transcript tương ứng được tô sáng và tự cuộn vào tầm nhìn. Việc **đọc nội dung** (gỡ băng) không xử lý bằng thuật toán cục bộ mà được ủy thác cho mô hình AI; tệp audio được đọc dưới dạng mảng byte, mã hóa Base64 và đính kèm vào yêu cầu gửi lên API.

**2.2.2. Gỡ băng (Speech-to-text) bằng mô hình đa phương thức**

Thay vì dùng một engine ASR riêng biệt, hệ thống gửi **trực tiếp dữ liệu audio** lên Gemini cùng một chỉ dẫn yêu cầu gỡ băng. Mô hình trả về transcript đã được định dạng theo dòng, kèm dấu thời gian và nhãn người nói/phân đoạn. Cách tiếp cận này đơn giản hóa kiến trúc, đồng thời cho chất lượng gỡ băng tốt nhờ khả năng hiểu ngữ cảnh của mô hình ngôn ngữ lớn.

**2.2.3. Google Gemini API và prompt engineering**

Google Gemini là họ mô hình ngôn ngữ lớn đa phương thức của Google, có khả năng tiếp nhận đồng thời văn bản, hình ảnh, âm thanh. API được truy cập qua giao thức REST tại điểm cuối `generativelanguage.googleapis.com`, sử dụng mô hình `gemini-2.5-flash` cho tốc độ và chi phí hợp lý. Yêu cầu và phản hồi đều ở định dạng JSON: phần `contents` chứa các "part" (văn bản hoặc dữ liệu nhị phân `inline_data`), phần `generationConfig` cho phép ép định dạng đầu ra (ví dụ `responseMimeType: application/json`).

*Prompt engineering* là kỹ thuật then chốt: bằng cách mô tả rõ vai trò ("bạn là chuyên gia ra đề IELTS"), liệt kê các dạng câu hỏi hợp lệ, quy định **lược đồ JSON** đầu ra mong muốn và đưa transcript vào ngữ cảnh, hệ thống "lập trình" cho mô hình sinh ra dữ liệu có cấu trúc, dễ phân tích cú pháp và bám sát yêu cầu.

**2.2.4. Cấu trúc câu hỏi trắc nghiệm tiêu chuẩn**

Một câu hỏi trong hệ thống được mô hình hóa với các thuộc tính: nội dung câu hỏi, dạng câu hỏi (`mcq`, `true_false`, `true_false_ng`, `fill_blank`, `sentence_completion`, `short_answer`, `matching`, `table_completion`, `note_completion`), danh sách lựa chọn (với câu trắc nghiệm/nối), đáp án đúng, đoạn trích dẫn transcript minh chứng và lời giải thích. Việc chuẩn hóa cấu trúc giúp lưu trữ thống nhất và hiển thị nhất quán trên nhiều màn hình.

**2.2.5. Lưu trữ và quản lý ngân hàng câu hỏi**

> *Ghi chú: Đề bài mẫu gợi ý lưu bằng file; sản phẩm thực tế lưu bằng cơ sở dữ liệu quan hệ nhúng để đảm bảo truy vấn linh hoạt và toàn vẹn dữ liệu.*

Hệ thống dùng **SQLite** – một hệ quản trị CSDL quan hệ nhúng, lưu toàn bộ dữ liệu trong một tệp `.db` duy nhất, không cần máy chủ riêng – kết nối qua trình điều khiển JDBC `sqlite-jdbc`. Mô hình dữ liệu gồm các bảng: `users` (người dùng, phân quyền), `question_banks` (đề/ngân hàng, gắn audio và transcript), `sections` (phân đoạn của đề), `questions` (câu hỏi), `student_results` (kết quả làm bài) và `saved_questions` (câu hỏi đã lưu để ôn tập). Cách tổ chức quan hệ này cho phép thực hiện các truy vấn như: liệt kê đề công khai, thống kê bảng xếp hạng, nhóm câu hỏi đã lưu theo từng đề nghe.

---

## CHƯƠNG 3: THUẬT TOÁN TRIỂN KHAI

### 3.1. Phát biểu bài toán

- **Đầu vào (Input):** Một tệp âm thanh tiếng Anh do người dùng cung cấp (định dạng MP3, WAV, M4A, OGG, FLAC, WEBM), kèm các tham số cấu hình: loại kỳ thi (IELTS/TOEIC/VSTEP/GENERAL), các dạng câu hỏi được chọn và số lượng câu cho mỗi dạng. Người dùng cũng có thể nạp transcript có sẵn thay vì gỡ băng tự động.
- **Đầu ra (Output):** Một bộ câu hỏi trắc nghiệm tiếng Anh có cấu trúc, mỗi câu gồm nội dung, đáp án đúng, đoạn trích transcript minh chứng và lời giải thích; toàn bộ được lưu thành một "ngân hàng câu hỏi" (question bank) trong cơ sở dữ liệu, sẵn sàng để làm bài, ôn tập và chia sẻ.

### 3.2. Thuật toán

Luồng xử lý chính của hệ thống (tính năng *Tạo đề*) được mô tả qua các bước sau:

**Bước 1 – Nạp tệp âm thanh.** Người dùng mở màn hình *Create Test*, nhấn chọn tệp qua hộp thoại `FileChooser`. Hệ thống lưu lại đường dẫn và cho phép phát thử để kiểm tra.

**Bước 2 – Gỡ băng (tùy chọn nhưng khuyến nghị).** Khi người dùng nhấn *Auto-Transcribe*, hệ thống đọc tệp audio thành mảng byte, mã hóa Base64 và gửi lên Gemini kèm chỉ dẫn gỡ băng. Phản hồi là transcript đã định dạng, được hiển thị trong ô văn bản để người dùng có thể chỉnh sửa.

**Bước 3 – Cấu hình câu hỏi.** Người dùng chọn kỳ thi, đánh dấu các dạng câu hỏi và số lượng. Ngoài cách cấu hình thủ công, hệ thống còn cung cấp **AI Agent**: người dùng gõ yêu cầu bằng ngôn ngữ tự nhiên (ví dụ *"Part 1 true/false 5 câu, Part 2 matching 5 câu"*), Agent phân tích và ánh xạ thành cấu hình từng phần.

**Bước 4 – Sinh câu hỏi.** Hệ thống tách transcript theo các Part, dựng prompt giàu ngữ cảnh cho từng dạng câu hỏi và gọi Gemini với cấu hình ép định dạng JSON. Trong lúc chờ, một màn hình "đang xử lý" được hiển thị để tránh người dùng thao tác nhầm.

**Bước 5 – Phân tích phản hồi.** Chuỗi JSON trả về được phân tích cú pháp bằng thư viện Gson thành danh sách đối tượng `Question`. Hệ thống kiểm tra, chuẩn hóa dạng câu hỏi và gán số thứ tự.

**Bước 6 – Rà soát và lưu trữ.** Kết quả hiển thị ở màn hình *Review* cùng khung transcript – audio để người dùng kiểm chứng từng câu (bấm vào đoạn trích để nghe đúng đoạn liên quan). Khi nhấn *Save*, hệ thống ghi đề (bank), phân đoạn (section) và từng câu hỏi vào CSDL trong một giao dịch (transaction) để đảm bảo toàn vẹn.

**Mô tả bằng mã giả (pseudocode):**

```
HÀM TaoDe(tepAudio, kyThi, danhSachDangCauHoi, soLuong):
    transcript ← GoBang(tepAudio)              // gọi Gemini, multimodal
    cacPart ← TachTheoPart(transcript)
    danhSachCauHoi ← []
    VỚI MỖI dang TRONG danhSachDangCauHoi:
        prompt ← DungPrompt(kyThi, dang, soLuong, transcript)
        phanHoiJson ← GoiGemini(prompt, epDinhDang = JSON)
        cauHoi ← PhanTichJson(phanHoiJson)     // Gson → List<Question>
        danhSachCauHoi.ThemTatCa(cauHoi)
    HIỂN_THỊ_REVIEW(danhSachCauHoi, transcript, audio)
    NẾU nguoiDung.Nhan("Save"):
        LuuVaoCSDL(bank, sections, danhSachCauHoi)   // transaction
    TRẢ VỀ danhSachCauHoi

HÀM GoBang(tepAudio):
    bytes ← DocTep(tepAudio)
    base64 ← Base64.Encode(bytes)
    yeuCau ← { contents: [ {text: "Gỡ băng đoạn audio..."},
                           {inline_data: {mime_type, data: base64}} ] }
    TRẢ VỀ GoiGemini(yeuCau).transcript
```

Sơ đồ luồng tổng quát: **Chọn audio → (Gỡ băng) → Cấu hình/AI Agent → Sinh câu hỏi (Gemini) → Parse JSON → Rà soát → Lưu CSDL → Sẵn sàng làm bài/ôn tập.**

---

## CHƯƠNG 4: CHƯƠNG TRÌNH VÀ KẾT QUẢ

### 4.1. Tổ chức chương trình

Dự án được tổ chức theo nguyên tắc **phân tách trách nhiệm** thành các gói (package) dưới `com.akatsuki`:

- **`App`** – Lớp khởi động ứng dụng JavaFX, khởi tạo cơ sở dữ liệu và hiển thị màn hình đăng nhập.
- **Gói `model`** – Các lớp dữ liệu thuần (POJO): `User` (người dùng, vai trò), `QuestionBank` (đề), `Section` (phân đoạn), `Question` (câu hỏi), `StudentResult` (kết quả), `RankingEntry` (dòng xếp hạng), `TranscriptLine` (dòng transcript có dấu thời gian).
- **Gói `database`** – Lớp `DatabaseManager` (mẫu Singleton) chịu trách nhiệm khởi tạo bảng và toàn bộ thao tác CRUD với SQLite: lưu/đọc đề, câu hỏi, kết quả, câu hỏi đã lưu, thống kê xếp hạng.
- **Gói `service`** – Tầng nghiệp vụ AI: `GeminiService` (gọi Gemini để gỡ băng và sinh câu hỏi), `AIAgentService` (diễn giải yêu cầu ngôn ngữ tự nhiên thành cấu hình câu hỏi), `PromptBuilder` (dựng prompt, tách transcript theo Part).
- **Gói `ui`** – Tầng giao diện JavaFX: `AuthView` (đăng nhập/đăng ký), `MainView` (khung điều hướng), `LibraryView` (thư viện đề), `CreateTestView` (tạo đề), `QuestionBankView` (lịch sử câu hỏi), `SavedQuestionsView` (câu hỏi đã lưu theo thư mục), `TestEngineView` (làm bài), `ResultsView` (tiến độ), `PracticeView` (luyện tập) và `TranscriptPane` (khung transcript đồng bộ audio).

Cách phân tầng này giúp giao diện không phụ thuộc trực tiếp vào chi tiết gọi API hay truy vấn SQL, thuận lợi cho việc bảo trì và mở rộng.

### 4.2. Ứng dụng học máy / AI trong chương trình

**Cách gọi API từ Java.** Hệ thống không dùng SDK bên thứ ba mà gọi **HTTP thuần** bằng `java.net.http.HttpClient` (có sẵn từ Java 11). Mỗi yêu cầu là một `HttpRequest` phương thức POST tới điểm cuối Gemini, tiêu đề `Content-Type: application/json`, thân là chuỗi JSON dựng bằng thư viện **Gson**. Khóa API được nạp từ tệp cấu hình `.env` trong tài nguyên ứng dụng, tránh việc viết cứng (hard-code) trong mã nguồn.

**Thiết kế prompt.** Lớp `PromptBuilder` chịu trách nhiệm tạo prompt theo từng dạng câu hỏi và kỳ thi, đồng thời đưa transcript (đã tách theo Part) vào ngữ cảnh. Với AI Agent, `AIAgentService` dùng một *system prompt* quy định mô hình phải trả về JSON đúng lược đồ `{message, configs[], ready}`; Agent được thiết kế để **hành động dứt khoát**, tự điền giá trị mặc định hợp lý (ví dụ 5 câu mỗi phần) thay vì hỏi lại những câu không cần thiết.

**Xử lý phản hồi.** Phản hồi JSON của Gemini được bóc tách qua đường dẫn `candidates[0].content.parts[0].text`, sau đó tiếp tục parse phần văn bản (vốn cũng là JSON nhờ `responseMimeType`) thành các đối tượng `Question`/`PartConfig`. Mọi bước parse đều bọc trong khối `try-catch` để chống lỗi định dạng.

**Xử lý lỗi và giới hạn API.** Hệ thống đặt thời gian chờ kết nối (timeout) cho mỗi yêu cầu; kiểm tra mã trạng thái HTTP (ví dụ phân biệt lỗi xác thực 400/403 với lỗi quá tải tạm thời 503); các tác vụ gọi mạng được chạy trên **luồng nền** (`new Thread`) và cập nhật giao diện qua `Platform.runLater`, nhờ đó giao diện không bị "đơ" trong lúc chờ AI phản hồi.

### 4.3. Ứng dụng kiểm thử

Nhóm đã thực hiện các ca kiểm thử tiêu biểu:

- **CT1 – Tệp audio hợp lệ:** Nạp tệp MP3 hội thoại ~3 phút → gỡ băng thành công, sinh được trọn bộ câu hỏi theo cấu hình. *Kết quả: Đạt.*
- **CT2 – Định dạng/tệp không hợp lệ:** Chọn tệp sai định dạng hoặc tệp rỗng → hệ thống báo lỗi rõ ràng ("Cannot play audio…", yêu cầu chọn lại tệp), không sập chương trình. *Kết quả: Đạt.*
- **CT3 – Lỗi/mất kết nối API:** Ngắt mạng hoặc dùng khóa API sai → bắt ngoại lệ, hiển thị thông báo thân thiện và cho phép thử lại; phân biệt được lỗi 503 (quá tải, thử lại sau) với lỗi khóa. *Kết quả: Đạt.*
- **CT4 – Transcript trống:** Bấm sinh câu hỏi khi chưa có transcript → hệ thống chặn và nhắc nhở. *Kết quả: Đạt.*
- **CT5 – Toàn vẹn dữ liệu khi lưu:** Lưu đề gồm nhiều câu trong một giao dịch; mô phỏng lỗi giữa chừng → giao dịch được rollback, không để lại dữ liệu rác. *Kết quả: Đạt.*
- **CT6 – Làm bài và chấm điểm:** Làm một đề, nộp bài → điểm số được tính đúng theo số câu đúng, lưu vào lịch sử và cập nhật bảng xếp hạng. *Kết quả: Đạt.*

### 4.4. Ngôn ngữ cài đặt

- **Ngôn ngữ:** Java (mã nguồn tương thích Java 17; môi trường biên dịch/chạy thực tế dùng OpenJDK).
- **Nền tảng giao diện:** JavaFX 21 (`javafx-controls`, `javafx-media`).
- **Thư viện ngoài:** `sqlite-jdbc` (CSDL SQLite), `gson` (xử lý JSON); gọi HTTP bằng `java.net.http` của thư viện chuẩn.
- **Công cụ build:** Apache Maven (plugin `javafx-maven-plugin`, lệnh chạy `mvn javafx:run`).
- **IDE & môi trường:** IntelliJ IDEA / VS Code; chạy trên hệ điều hành macOS/Windows/Linux có cài JDK.

### 4.5. Kết quả

#### 4.5.1. Giao diện chính của chương trình

> *Ghi chú: Giao diện được xây dựng bằng JavaFX (đề bài mẫu ghi Swing). Khi chèn ảnh chụp màn hình vào Word, đặt chú thích "Hình 4.x" dưới mỗi hình.*

- **Màn hình Đăng nhập/Đăng ký (`AuthView`):** Cho phép người dùng đăng nhập hoặc tạo tài khoản, chọn vai trò (giảng viên/sinh viên).
- **Khung điều hướng (`MainView`):** Thanh điều hướng trên cùng với logo, các mục Library, Create Test, Question Bank, Saved, My Progress (tùy theo vai trò), và thông tin người dùng.
- **Màn hình Tạo đề (`CreateTestView`):** Quy trình 3 bước (Upload → Configure → Review). Bước 1 là vùng kéo–thả/chọn tệp audio. Bước 2 gồm: thẻ phát thử audio, ô transcript với nút *Auto-Transcribe*, bảng cấu hình dạng câu hỏi theo kỳ thi, và **khung chat AI Agent** để ra lệnh bằng ngôn ngữ tự nhiên. Bước 3 hiển thị danh sách câu hỏi sinh ra bên cạnh khung transcript – audio để rà soát.
- **Thư viện đề (`LibraryView`):** Lưới các thẻ đề công khai (kèm bộ lọc theo kỳ thi), mỗi thẻ có nút *Start Test* và *Bảng xếp hạng*.
- **Ngân hàng câu hỏi (`QuestionBankView`):** Liệt kê lịch sử mọi câu hỏi đã tạo; nhấn vào một câu sẽ mở **panel lớn chứa audio + transcript** để nghe lại và lưu câu hỏi.
- **Câu hỏi đã lưu (`SavedQuestionsView`):** Các câu đã lưu được **nhóm thành thư mục theo từng đề nghe**, mỗi thư mục có audio và transcript riêng để ôn tập.
- **Màn hình Làm bài (`TestEngineView`):** Khung transcript – audio đồng bộ, danh sách câu hỏi, bộ điều hướng câu, nút nộp bài và màn hình xem lại kết quả kèm giải thích.

#### 4.5.2. Kết quả thực thi

Qua thực nghiệm với nhiều đoạn audio khác nhau, hệ thống gỡ băng và sinh câu hỏi ổn định. Với một đoạn audio vài phút, hệ thống có thể sinh hàng chục câu hỏi thuộc nhiều dạng chỉ trong khoảng vài chục giây (phụ thuộc tải của máy chủ Gemini). Câu hỏi sinh ra bám sát nội dung audio, có đáp án và đoạn trích dẫn minh chứng cho phép người học truy ngược về đúng đoạn nghe. Chức năng làm bài chấm điểm chính xác, lưu lịch sử và hiển thị bảng xếp hạng theo điểm cao nhất của mỗi người học.

#### 4.5.3. Nhận xét đánh giá

**Ưu điểm:** Tự động hóa toàn bộ quy trình từ audio đến ngân hàng câu hỏi; đa dạng dạng câu hỏi theo nhiều kỳ thi; trải nghiệm khép kín (tạo đề – làm bài – ôn tập – xếp hạng); kiến trúc phân tầng rõ ràng; xử lý lỗi và đa luồng giúp giao diện mượt mà; tính năng đồng bộ audio – transcript hỗ trợ học tập hiệu quả.

**Hạn chế:** Phụ thuộc vào kết nối mạng và dịch vụ Gemini (chịu ảnh hưởng khi API quá tải); chất lượng câu hỏi tuy tốt nhưng vẫn cần con người rà soát; hệ thống hiện chạy trên máy đơn, chưa có đồng bộ đám mây; chưa hỗ trợ xuất đề ra các định dạng tài liệu phổ biến.

---

## CHƯƠNG 5: KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

### 5.1. Kết luận

Đồ án đã hoàn thành mục tiêu đề ra: xây dựng thành công một hệ thống ngân hàng câu hỏi luyện nghe tiếng Anh, có khả năng tự động gỡ băng tệp âm thanh và sinh câu hỏi trắc nghiệm đa dạng bằng Google Gemini API. Hệ thống không chỉ dừng ở việc sinh câu hỏi mà còn cung cấp một hệ sinh thái học tập tương đối hoàn chỉnh: quản lý ngân hàng câu hỏi, thư viện đề công khai, làm bài có chấm điểm và đồng bộ audio – transcript, lưu trữ câu hỏi ôn tập theo thư mục, theo dõi tiến độ và bảng xếp hạng. Quá trình thực hiện giúp nhóm củng cố vững chắc kiến thức về lập trình hướng đối tượng với Java, thiết kế kiến trúc phân tầng, lập trình mạng và xử lý JSON, lập trình đa luồng, thiết kế giao diện JavaFX, cũng như kỹ thuật tích hợp và điều khiển mô hình AI bằng prompt engineering.

### 5.2. Hướng phát triển

Trong tương lai, hệ thống có thể được mở rộng theo nhiều hướng: (i) **hỗ trợ thêm định dạng** đầu vào (video, link YouTube, podcast); (ii) **nâng cao chất lượng câu hỏi** bằng cách tinh chỉnh prompt, thêm bước AI tự kiểm định độ khó và tính hợp lý của đáp án; (iii) **xuất đề thi** ra các định dạng PDF/DOCX/Word và phiếu trả lời; (iv) **triển khai nền tảng web/đám mây** để nhiều người dùng truy cập đồng thời, đồng bộ dữ liệu và lớp học trực tuyến; (v) **phân tích học tập** sâu hơn với thống kê điểm yếu của người học theo dạng câu hỏi nhằm gợi ý lộ trình luyện tập cá nhân hóa; (vi) **hỗ trợ đa ngôn ngữ** để áp dụng cho việc luyện nghe các ngoại ngữ khác.

---

## TÀI LIỆU THAM KHẢO

[1] Google, *Gemini API documentation – Generate content & multimodal inputs*, Google AI for Developers. https://ai.google.dev/gemini-api/docs

[2] Oracle, *The Java Tutorials* & *Java Platform, Standard Edition API Specification* (`java.net.http.HttpClient`). https://docs.oracle.com/en/java/

[3] OpenJFX, *JavaFX Documentation and API – javafx.media, javafx.controls*. https://openjfx.io/

[4] SQLite Consortium, *SQLite Documentation*. https://www.sqlite.org/docs.html & Xerial, *sqlite-jdbc Driver*. https://github.com/xerial/sqlite-jdbc

[5] Google, *Gson User Guide*. https://github.com/google/gson

[6] J. White et al., *"A Prompt Pattern Catalog to Enhance Prompt Engineering with ChatGPT/LLMs"*, arXiv:2302.11382, 2023.

[7] British Council & IELTS Partners, *IELTS Listening – Question types and band descriptors*. https://www.ielts.org

---

## PHỤ LỤC

*Phần này dành để đính kèm source code — sinh viên tự bổ sung.*

(Gợi ý: đính kèm mã nguồn các lớp chính như `GeminiService`, `AIAgentService`, `PromptBuilder`, `DatabaseManager`, `CreateTestView`, `TestEngineView`, kèm hướng dẫn build/chạy: `cd akatsukiapp && mvn javafx:run`.)
