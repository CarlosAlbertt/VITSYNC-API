import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashSecret {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("HASH FOR SECRET: " + encoder.encode("secret"));
    }
}
