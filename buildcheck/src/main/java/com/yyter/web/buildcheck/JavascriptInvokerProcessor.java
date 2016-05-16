package com.yyter.web.buildcheck;

import com.yyter.web.JavascriptInvoker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * Created by liyang on 5/16/16.
 */
public class JavascriptInvokerProcessor extends AbstractProcessor {
    private Elements elementsUtil;
    private Messager messager;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elementsUtil = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        boolean handled = false;
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(JavascriptInvoker.class);
        for(Element element : elements) {
            if(element.getKind() == ElementKind.METHOD) {
                Set<Modifier> modifiers = element.getModifiers();
                if(!modifiers.contains(Modifier.PUBLIC)) {
                    error("@JavascriptInvoker can noly used by public method"
                            + ", Found: " + getName(element.getEnclosingElement()) + "." + element.toString());
                    return true;
                }

                ExecutableType executableType = (ExecutableType) element.asType();
                List<? extends TypeMirror> typeMirrors = executableType.getParameterTypes();
                if(typeMirrors.isEmpty()) {
                    //ok
                    continue;
                }
                for(TypeMirror typeMirror : typeMirrors) {
                    if(!String.class.getName().equals(typeMirror.toString())) {
                        error("method annotated by @JavascriptInvoker should have no param or String params, found param: "
                                + typeMirror.toString() + ", Found: " + getName(element.getEnclosingElement()) + "." + element.toString());
                        return true;
                    }
                }
                handled = true;
            } else {
                error("@JavascriptInvoker can noly used by method. " + element.toString());
                return true;
            }
        }
        return handled;
    }

    private void error(String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    private String getName(Element element) {
        if(element instanceof TypeElement) {
            return elementsUtil.getBinaryName((TypeElement) element).toString();
        }

        return element.getSimpleName().toString();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> annos = new HashSet<>(1);
        annos.add(JavascriptInvoker.class.getName());
        return annos;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
