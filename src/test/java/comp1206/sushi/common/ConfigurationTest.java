package comp1206.sushi.common;

import org.junit.Test;

public class ConfigurationTest {
    @Test
    public void testParsing() {
       Configuration config = new Configuration("Configuration.txt");
        assert config.getDishes().get("Sushi Roll").getDescription().equals("Rice and things");
        assert config.getPostcodes().get("SO17 1BJ") != null;
        assert config.getIngredients().size() == 4;
        assert config.getUsers().size() == 1;
    }
}