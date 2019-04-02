package comp1206.sushi.common;

import org.junit.Test;

public class ConfigurationTest {
    @Test
    public void testParsing() {
       Configuration config = new Configuration("Configuration.txt");
        assert config.getDishes().get(0).getDescription().equals("Rice and things");
        assert config.getPostcodes().get(0) != null;
        assert config.getIngredients().size() == 4;
        assert config.getUsers().size() == 1;
    }
}