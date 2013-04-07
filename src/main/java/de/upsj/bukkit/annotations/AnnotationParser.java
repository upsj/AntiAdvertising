package de.upsj.bukkit.annotations;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class AnnotationParser implements Processor {
    public static final String PLUGIN_YAML = "target/plugin.yml";
    public static final String CONFIG_DOC = "target/config.html";

    private Types types;
    private Elements elements;

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(Arrays.asList(ConfigSection.class.getName(),
                Plugin.class.getName(), Permission.class.getName(), CommandDef.class.getName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_6;
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            // second round
            return true;
        }

        // read config sections
        Map<String, ConfigSection> configSections = new HashMap<String, ConfigSection>();
        for (Element e : roundEnv.getElementsAnnotatedWith(ConfigSection.class)) {
            assert e.getKind() == ElementKind.CLASS : "non-class annotated with ConfigSection";
            assert e instanceof TypeElement : "inconsistency";
            TypeElement el = (TypeElement) e;
            configSections.put(getClass(el), el.getAnnotation(ConfigSection.class));
        }

        // read permissions
        Map<String, Permission> permissions = new HashMap<String, Permission>();
        for (Element e : roundEnv.getElementsAnnotatedWith(Permission.class)) {
            assert e.getKind() == ElementKind.FIELD : "non-field annotated with Permission";
            assert e instanceof VariableElement : "inconsistency";
            VariableElement el = (VariableElement) e;
            assert el.getConstantValue() instanceof String : "permission is no string";
            permissions.put((String) el.getConstantValue(), el.getAnnotation(Permission.class));
        }

        // read command definitions
        List<CommandDef> commands = new ArrayList<CommandDef>();
        for (Element e : roundEnv.getElementsAnnotatedWith(CommandDef.class)) {
            assert e.getKind() == ElementKind.CLASS : "non-class annotated with CommandDef";
            assert e instanceof TypeElement : "inconsistency";
            commands.add(e.getAnnotation(CommandDef.class));
        }

        // read plugin
        Plugin pluginAnnotation;
        String pluginClass;
        {
            Set<? extends Element> pluginAnnotated = roundEnv.getElementsAnnotatedWith(Plugin.class);
            assert pluginAnnotated.size() == 1 : "only one plugin expected, found " + pluginAnnotated.toString();
            Element e = pluginAnnotated.iterator().next();
            assert e.getKind() == ElementKind.CLASS : "non-class annotated with Plugin";
            assert e instanceof TypeElement : "inconsistency";
            TypeElement el = (TypeElement) e;
            pluginClass = getClass(el);
            assert types.isSubtype(el.asType(), getTypeMirror(JavaPlugin.class))
                    : "class annotated with Plugin doesn't override JavaPlugin";
            pluginAnnotation = el.getAnnotation(Plugin.class);
        }

        printPluginYAML(pluginAnnotation, pluginClass, permissions, commands);
        printConfigDoc(configSections);
        return true;
    }

    private void printPluginYAML(Plugin pluginAnnotation, String pluginClass, Map<String, Permission> permissions, List<CommandDef> commands) {
        try {
            YamlWriter yaml = new YamlWriter(new FileWriter(PLUGIN_YAML));
            yaml.write("name", pluginAnnotation.name());
            yaml.write("main", pluginClass);
            yaml.write("description", pluginAnnotation.description());
            yaml.write("version", pluginAnnotation.version());
            yaml.write("author", pluginAnnotation.author());
            {
                yaml.writeOpen("commands");
                for (CommandDef command : commands) {
                    yaml.writeOpen(command.name());
                    // TODO write aliases
                    yaml.write("description", command.description());
                    assert permissions.containsKey(command.permission())
                            : "unknown permission " + command.permission() + " for command " + command.name();
                    yaml.write("permission", command.permission());
                    yaml.writeClose();
                }
                yaml.writeClose();
            }

            {
                yaml.writeOpen("permissions");
                for (Map.Entry<String, Permission> entry : permissions.entrySet()) {
                    yaml.writeOpen(entry.getKey());
                    yaml.write("description", entry.getValue().value());
                    yaml.write("default", entry.getValue().defaultPerm().getValue());
                    yaml.writeClose();
                }
                yaml.writeClose();
            }

            yaml.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printConfigDoc(Map<String, ConfigSection> configSections) {
        try {
            HTMLWriter writer = new HTMLWriter(new FileWriter(CONFIG_DOC));
            // TODO
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TypeMirror getTypeMirror(Class<?> typeClass) {
        // TODO look for better solution
        return elements.getTypeElement(typeClass.getName()).asType();
    }

    private static String getClass(TypeElement el) {
        return el.getQualifiedName().toString();
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return Collections.emptyList();
    }

    private class YamlWriter {
        private final FileWriter writer;
        private String ident;

        YamlWriter(FileWriter writer) {
            this.writer = writer;
            this.ident = "";
        }

        private void writeKey(String key, boolean space) throws IOException {
            writer.write(ident + yamlString(key) + ":" + (space ? " " : ""));
        }

        private String yamlString(String s) {
            // safe implementation to avoid checks
            return s.matches("[A-Za-z0-9\\.]+")
                    ? s
                    : "'" + s.replace("'", "\\'") + "'";
        }

        public void write(String key, String value) throws IOException {
            writeKey(key, true);
            writer.write(yamlString(value));
            newLine();
        }

        private void newLine() throws IOException {
            writer.write('\n');
        }

        public void writeOpen(String key) throws IOException {
            writeKey(key, false);
            newLine();
            ident += "  ";
        }

        public void writeClose() {
            ident = ident.substring(2);
        }

        public void finish() throws IOException {
            writer.flush();
            writer.close();
        }
    }

    private class HTMLWriter {
        private final FileWriter writer;

        HTMLWriter(FileWriter writer) {
            this.writer = writer;
        }

        // TODO
    }
}
