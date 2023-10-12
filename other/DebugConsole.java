package other;

import javax.tools.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DebugConsole extends Frame {
    //This is debug classloader and should not be used in public version of the game by any means.

    // use this to expose objects for debugging
//    private DebugConsole debugConsole;
//    try {
//        initializeDebugConsole();
//    } catch (Exception e){
//        e.printStackTrace();
//    }
//    private void initializeDebugConsole() {
//        debugConsole = new DebugConsole();
//        debugConsole.setVisible(true);
//        debugConsole.exposeObject("cookbook", this);
//    }

    private TextArea codeInputArea;
    private Button executeButton;
    private Map<String, Object> exposedObjects = new HashMap<>();

    public DebugConsole() {
        setLayout(new BorderLayout());
        setTitle("Debug Console");
        setSize(400, 300);

        codeInputArea = new TextArea();
        executeButton = new Button("Execute Code");

        add(codeInputArea, BorderLayout.CENTER);
        add(executeButton, BorderLayout.SOUTH);

        executeButton.addActionListener(e -> executeJavaCode(codeInputArea.getText()));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.exit(0);
            }
        });
    }

    public void exposeObject(String varName, Object object) {
        exposedObjects.put(varName, object);
    }

    private void executeJavaCode(String code) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        StringBuilder injectedCode = new StringBuilder();
        for (Map.Entry<String, Object> entry : exposedObjects.entrySet()) {
            String className = entry.getValue().getClass().getName();
            injectedCode.append(className).append(" ").append(entry.getKey()).append(" = ")
                    .append("((").append(className).append(")exposedObjects.get(\"")
                    .append(entry.getKey()).append("\"));");
        }

        String fullCode = String.format(
                "import java.util.Map;" +
                        "public class DynamicExecutor {" +
                        "    public static void execute(Map<String, Object> exposedObjects) {" +
                        "        %s" +
                        "        %s" +
                        "    }" +
                        "}", injectedCode, code);

        JavaFileObject file = new InMemoryJavaFileObject("DynamicExecutor", fullCode);
        Iterable<? extends JavaFileObject> files = Arrays.asList(file);

        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        compiler.getTask(out, fileManager, null, null, null, files).call();

        if (writer.toString().length() > 0) {
            System.err.println("Compilation error:");
            System.err.println(writer);
            return;
        }

        try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File("").toURI().toURL()})) {
            Class<?> cls = Class.forName("DynamicExecutor", true, classLoader);
            Method method = cls.getMethod("execute", Map.class);
            method.invoke(null, exposedObjects);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileManager.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        protected InMemoryJavaFileObject(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
