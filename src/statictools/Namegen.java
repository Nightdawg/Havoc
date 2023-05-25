package statictools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Namegen {
    public static HashMap<Long, String> adjectives;
    public static HashMap<Long, String> names;
    
    public static String getName(long i) {
        long adjindex = i%(long)adjectives.size();
        long nameindex = i%(long)names.size();
        String adj = adjectives.get(adjindex);
        String name = names.get(nameindex);
        String genname = adj+" "+name;
        return genname;
    }

    static {
        adjectives = readWords("res/namegen/adjectives.txt");
        names = readWords("res/namegen/names.txt");
    }

    private static HashMap<Long, String> readWords(String file) {
        HashMap<Long, String> map = new HashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                map.put((long) index, line);
                index++;
            }
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) { // ignored
                }
            }
        }
        return map;
    }
}
