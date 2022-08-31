package example.springbootdynamodb.config;


import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import example.springbootdynamodb.Application;
import example.springbootdynamodb.properties.DynamoTableProperties;
import example.springbootdynamodb.repository.DynamoDbRepository;
import example.springbootdynamodb.repository.IDynamoDbRepository;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = Application.class)
@ActiveProfiles("dev")
@Slf4j
//@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractBaseTest {

  private static final String dynamoDbLocalPort = DynamoDBLocalUtil.getFreePortString();
  @Autowired
  public ObjectMapper objectMapper;
  @Autowired
  public DynamoDbClient dynamoDbClient;
  @LocalServerPort
  protected int serverPort;
  @Autowired
  private DynamoTableProperties dynamoTableProperties;
  @Autowired
  private ApplicationContext applicationContext;

//  @Autowired
//  private List<IDynamoDbRepository<? extends DynamoDbRepository>> allTables;
//
  @DynamicPropertySource
  public static void configureDynamoDbLocalServer(DynamicPropertyRegistry registry) {
    registry.add("dynamodblocal.server.port", () -> dynamoDbLocalPort);
  }

  @BeforeAll
  public static void beforeAll() {
    DynamoDBLocalUtil.createProxyServer(dynamoDbLocalPort);
  }

  @BeforeEach
  public void setupBaseTest() {
    configureRestAssured();
  }

  private void configureRestAssured() {
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    RestAssured.config =
        RestAssuredConfig.config()
            .objectMapperConfig(
                new ObjectMapperConfig()
                    .jackson2ObjectMapperFactory((cls, charset) -> objectMapper))
            .logConfig(
                LogConfig.logConfig()
                    .enableLoggingOfRequestAndResponseIfValidationFails()
                    .enablePrettyPrinting(true));

    RestAssured.requestSpecification =
        new RequestSpecBuilder()
            .build()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .config(RestAssured.config());
  }

  @AfterEach
  public <T> void cleanUp() {
    RestAssured.reset();
    applicationContext.getBeansOfType(DynamoDbRepository.class).values().parallelStream()
        .forEach(repo -> {
          Iterator<T> iterator = repo.scan().items().iterator();
          while (iterator.hasNext()) {
            repo.deleteItem(iterator.next());
          }
    });
  }


}
