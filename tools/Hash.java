import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 一次性工具：生成演示账号统一的 BCrypt 密码哈希（密码 123456）
 * 运行: java -cp <spring-security-crypto.jar> tools/Hash.java
 */
public class Hash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = args.length > 0 ? args[0] : "123456";
        String hash = encoder.encode(raw);
        System.out.println(hash);
        System.out.println("verify=" + encoder.matches(raw, hash));
    }
}
