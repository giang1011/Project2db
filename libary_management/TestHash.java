import org.mindrot.jbcrypt.BCrypt;

public class TestHash {
    public static void main(String[] args) {
        String pw = "123456";
        String hash1 = "$2a$10$Xm4zH0u94tXmJp/10i5vOe08vj.9dC7lUu8UvS1Q6mE.L/Z2qXo/G";
        System.out.println("Checking hash: " + BCrypt.checkpw(pw, hash1));
        System.out.println("New hash: " + BCrypt.hashpw(pw, BCrypt.gensalt()));
    }
}
