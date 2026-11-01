package com.meridian;

import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HexagonalArchitectureTest {
 @TempDir Path output;
 @Test void innerLayersCompileWithoutSpringJdbcHttpOrLegacyClasses() throws Exception {
  var root=Path.of("src/main/java/com/meridian");
  List<String> files;
  try(var paths=Files.walk(root)){
   files=paths.filter(p->p.toString().endsWith(".java"))
    .filter(p->p.toString().contains("/domain/")||p.toString().contains("/application/"))
    .map(Path::toString).toList();
  }
  assertTrue(files.size()>=10,"Architecture guard must discover migrated core classes");
  var compiler=ToolProvider.getSystemJavaCompiler();
  assertNotNull(compiler,"Run architecture checks with a JDK");
  var args=new ArrayList<>(List.of("--release","21","-proc:none","-classpath",output.toString(),"-sourcepath",output.toString(),"-d",output.toString()));
  args.addAll(files);
  assertEquals(0,compiler.run(null,null,null,args.toArray(String[]::new)),"Core must compile using only the JDK and inner-layer code");
 }
 @Test void domainCompilesWithoutApplicationLayer() throws Exception {
  List<String> files;
  try(var paths=Files.walk(Path.of("src/main/java/com/meridian"))){files=paths.filter(p->p.toString().endsWith(".java")&&p.toString().contains("/domain/")).map(Path::toString).toList();}
  assertFalse(files.isEmpty());
  var args=new ArrayList<>(List.of("--release","21","-proc:none","-classpath",output.toString(),"-sourcepath",output.toString(),"-d",output.toString()));args.addAll(files);
  assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new)),"Domain may not depend on application ports or adapters");
 }
}
