package org.archagents.metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

/**
 * CK 0.7.0 predates Java records: its visitor has no RecordDeclaration case, so a record yields no
 * class result and CK's metric visitors throw on the file (losing every type in it). Before CK runs,
 * each record is therefore rewritten into the class a developer would have written before records:
 *
 * <pre>
 *   record R&lt;T&gt;(A a, B b) implements I { body }
 *   =&gt; final class R&lt;T&gt; implements I { private final A a; private final B b; body }
 * </pre>
 *
 * A compact constructor becomes a constructor with the component parameters. Implicit members
 * (accessors, equals, hashCode, toString) are not added: they are not written code. The rewrite is
 * done on a copy of the source tree; the snapshot itself is never touched.
 */
final class RecordDesugar {

    private RecordDesugar() {
    }

    /** Copies every .java file under {@code src} into {@code dst}, desugaring records. Returns the number rewritten. */
    static int copy(Path src, Path dst) throws IOException {
        int rewritten = 0;
        List<Path> files;
        try (var s = MetricsCli.javaFiles(src)) {
            files = s.toList();
        }
        for (Path f : files) {
            String text = Files.readString(f, StandardCharsets.UTF_8);
            String out = text;
            for (int pass = 0; pass < 16; pass++) {
                String next = rewriteOutermostRecords(out);
                if (next == null) {
                    break;
                }
                out = next;
            }
            if (!out.equals(text)) {
                rewritten++;
            }
            Path target = dst.resolve(src.relativize(f).toString());
            Files.createDirectories(target.getParent());
            Files.writeString(target, out, StandardCharsets.UTF_8);
        }
        return rewritten;
    }

    static ASTParser parser() {
        ASTParser p = ASTParser.newParser(AST.JLS21);
        p.setKind(ASTParser.K_COMPILATION_UNIT);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
        p.setCompilerOptions(options);
        return p;
    }

    /** Returns the source with all outermost records replaced, or null if there is none. */
    @SuppressWarnings("unchecked")
    private static String rewriteOutermostRecords(String source) {
        ASTParser p = parser();
        p.setSource(source.toCharArray());
        CompilationUnit cu = (CompilationUnit) p.createAST(null);
        List<RecordDeclaration> records = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(RecordDeclaration node) {
                records.add(node);
                return false;
            }
        });
        if (records.isEmpty()) {
            return null;
        }
        AST ast = cu.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        for (RecordDeclaration rec : records) {
            TypeDeclaration td = ast.newTypeDeclaration();
            td.setName((SimpleName) ASTNode.copySubtree(ast, rec.getName()));
            boolean isFinal = false;
            for (Object m : rec.modifiers()) {
                td.modifiers().add(ASTNode.copySubtree(ast, (ASTNode) m));
                isFinal |= m instanceof Modifier mod && mod.isFinal();
            }
            if (!isFinal) {
                td.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
            }
            for (Object t : rec.typeParameters()) {
                td.typeParameters().add(ASTNode.copySubtree(ast, (ASTNode) t));
            }
            for (Object t : rec.superInterfaceTypes()) {
                td.superInterfaceTypes().add(ASTNode.copySubtree(ast, (ASTNode) t));
            }
            List<SingleVariableDeclaration> components = rec.recordComponents();
            for (SingleVariableDeclaration c : components) {
                VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
                frag.setName((SimpleName) ASTNode.copySubtree(ast, c.getName()));
                FieldDeclaration fd = ast.newFieldDeclaration(frag);
                Type type = (Type) ASTNode.copySubtree(ast, c.getType());
                fd.setType(c.isVarargs() ? ast.newArrayType(type) : type);
                fd.modifiers().addAll(ast.newModifiers(Modifier.PRIVATE | Modifier.FINAL));
                td.bodyDeclarations().add(fd);
            }
            for (Object b : rec.bodyDeclarations()) {
                BodyDeclaration copy = (BodyDeclaration) ASTNode.copySubtree(ast, (ASTNode) b);
                if (b instanceof MethodDeclaration md && md.isCompactConstructor()) {
                    MethodDeclaration ctor = (MethodDeclaration) copy;
                    ctor.setCompactConstructor(false);
                    for (SingleVariableDeclaration c : components) {
                        ctor.parameters().add(ASTNode.copySubtree(ast, c));
                    }
                }
                td.bodyDeclarations().add(copy);
            }
            rewrite.replace(rec, td, null);
        }
        Document doc = new Document(source);
        try {
            rewrite.rewriteAST(doc, null).apply(doc);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        return doc.get();
    }
}
