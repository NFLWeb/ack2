package com.dank.analysis.impl.widget;

import com.dank.analysis.impl.client.visitor.RunescriptOpcodeHandlerVisitor;
import com.dank.analysis.impl.widget.visitor.MarginVisitor;
import com.dank.analysis.impl.widget.visitor.RuneScriptVisitor;
import com.dank.hook.RSMethod;
import com.dank.util.Wildcard;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.cfg.BasicBlock;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeTree;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.util.TreeBuilder;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.dank.DankEngine;
import com.dank.analysis.Analyser;
import com.dank.analysis.impl.widget.visitor.GenericVisitor;
import com.dank.hook.Hook;
import com.dank.hook.RSField;
import com.dank.util.Filter;

import java.lang.reflect.Modifier;

/**
 * Project: DankWise
 * Time: 18:04
 * Date: 13-02-2015
 * Created by Dogerina.
 */
public class Widget extends Analyser {

    public static void postAnalysis() {
        final ClassNode widget = Hook.WIDGET.resolve();
        for (final FieldNode fn : widget.fields) {
            if (!fn.isStatic()) {
                if (fn.desc.equals("[Ljava/lang/String;") && !fn.name.equals(Hook.WIDGET.get("actions").name)) {
                    Hook.WIDGET.put(new RSField(fn, "tableActions"));
                }
            }
        }
        for (final ClassNode cn : DankEngine.classPath) {
            for (final MethodNode mn : cn.methods) {
                bipush(mn, 100, "Z", Opcodes.PUTFIELD, Hook.WIDGET, "interactable", 0, new Filter<FieldInsnNode>() {
                    @Override
                    public boolean accepts(FieldInsnNode fin) {
                        return !fin.name.equals(Hook.WIDGET.get("hidden").name);
                    }
                });
            }
        }
    }

    private static void bipush(final MethodNode mn, final int val, final String desc, final int opcode, final Hook dest,
                               final String mnemonic, final int skips, final Filter<FieldInsnNode> filter) {
        for (final AbstractInsnNode ain : mn.instructions.toArray()) {
            if (ain.opcode() == Opcodes.BIPUSH) {
                final IntInsnNode iin = (IntInsnNode) ain;
                if (iin.operand != val) continue;
                final FieldInsnNode fin = next(ain, opcode, "Z", dest.getInternalName(), 0);
                if (fin != null && (filter == null || filter.accepts(fin))) {
                    dest.put(new RSField(fin, mnemonic));
                }
            }
        }
    }

    private static FieldInsnNode next(AbstractInsnNode from, final int op, final String desc, final String owner, final int skips) {
        int skipped = 0;
        while ((from = from.next()) != null) {
            if (from.opcode() == op) {
                final FieldInsnNode topkek = (FieldInsnNode) from;
                if (topkek.desc.equals(desc) && (owner == null || owner.equals(topkek.owner))) {
                    if (skipped == skips) {
                        return topkek;
                    } else {
                        skipped++;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ClassSpec specify(ClassNode cn) {
        return cn.fieldCount(Object[].class) <= 5 || cn.fieldCount(int.class) <= 30 ? null : new ClassSpec(Hook.WIDGET, cn);
    }

    @Override
    public void evaluate(ClassNode cn) {

        for (final MethodNode mn : cn.methods) {
            if (!mn.isStatic()) {
                final NodeTree tree = TreeBuilder.build(mn);
                tree.accept(new GenericVisitor());
                tree.accept(new FontCacheVisitor());
            }
        }
        for (final FieldNode fn : cn.fields) {
            if (!fn.isStatic()) {
                if (fn.desc.equals(Hook.WIDGET.getInternalArrayDesc())) {
                    Hook.WIDGET.put(new RSField(fn, "children"));
                } else if (fn.desc.equals(Hook.WIDGET.getInternalDesc())) {
                    Hook.WIDGET.put(new RSField(fn, "parent"));
                }
            } else if (fn.isStatic()) {
                if (fn.desc.equals(Type.getDescriptor(boolean[].class))) {
                    Hook.CLIENT.put(new RSField(fn, "loadedWindows"));
                }
            }
        }

        NodeVisitor rsohv = new RunescriptOpcodeHandlerVisitor();

        for (ClassNode c : DankEngine.classPath.getClasses()) {
            for (MethodNode mn : c.methods) {
                if (new Wildcard("(" + Hook.SCRIPT_EVENT.getInternalDesc() + "I?)V").matches(mn.desc)) {
                    System.out.println("??" + mn.key());
                    Hook.CLIENT.put(new RSMethod(mn, "runScript"));
                    TreeBuilder.build(mn).accept(new MarginVisitor());
                    TreeBuilder.build(mn).accept(new RuneScriptVisitor());
                }
                TreeBuilder.build(mn).accept(rsohv);

            }
        }
    }

    public class FontCacheVisitor extends NodeVisitor {

        @Override
        public void visitField(FieldMemberNode fmn) {
//            System.out.println(Hook.FONT_IMPL.getInternalName());
            if (new Wildcard("(I)" + Hook.FONT_IMPL.getInternalDesc()).matches(fmn.method().desc)) {
                if (fmn.isStatic() && fmn.desc().equals(Hook.CACHE_TABLE.getInternalDesc())) {
                    Hook.CLIENT.put(new RSField(fmn, "fontCache"));
                }
            }
        }
    }
}
