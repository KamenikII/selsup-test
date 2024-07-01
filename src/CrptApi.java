import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Semaphore semaphore;

    public CrptApi(
            TimeUnit timeUnit,
            int requestLimit
    ) {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    public void createDoc(
            String url,
            Document doc,
            String signature
    ) {
        try {
            if (!semaphore.tryAcquire()) {
                System.err.println("Limits exceeded");
                return;
            }

            String requestBody = mapper.writeValueAsString(doc);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Doc is created");
            } else {
                System.err.println("Error. HTTP-status: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    public class Document
    {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        public Document(Description description, String doc_id, String doc_status, String doc_type,
                        boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                        String production_date, String production_type, List<Product> products, String reg_date,
                        String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        public static class Description
        {
            private String participantInn;
        }
        public static class Product
        {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        Document document = crptApi.new Document();
        String signature = "example_signature";
        crptApi.createDoc(
                "https://ismp.crpt.ru/api/v3/lk/documents/create",
                document,
                signature
        );
    }
}