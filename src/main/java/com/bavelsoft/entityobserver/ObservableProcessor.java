package com.bavelsoft.entityobserver;

import static java.util.Arrays.asList;
import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.type.TypeKind.VOID;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.NATIVE;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.Arrays;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.annotation.processing.Filer;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.tools.Diagnostic;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.ExecutableType;
import com.google.auto.service.AutoService;


@AutoService(Processor.class)
public class ObservableProcessor extends AbstractProcessor {
	private Messager messager;
	private Elements elementUtils;
	private Filer filer;

	@Override
	public synchronized void init(ProcessingEnvironment env){
		super.init(env);
		messager = env.getMessager();
		elementUtils = env.getElementUtils();
		filer = env.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotationsParam, RoundEnvironment env) {
		for (Element element : env.getElementsAnnotatedWith(Observable.class)) {
			try {
				process(element);
			} catch (IOException e) {
                        	throw new RuntimeException(e);
                	}
		}
		return true;
	}

	private void process(Element element) throws IOException {
		String packageName = elementUtils.getPackageOf(element).toString();
//TODO inner class support
		TypeName underlyingType = TypeName.get(element.asType());
		TypeName observersType = ArrayTypeName.of(Observer.class);
		TypeName beforeValuesType = ArrayTypeName.of(Object.class);
		MethodSpec beforeChange = MethodSpec.methodBuilder("beforeChange")
			.addModifiers(PRIVATE)
			.beginControlFlow("for (int i=0; i<observers.length; i++)")
			.addStatement("beforeValues[i] = observers[i].beforeChange(underlying)")
			.endControlFlow()
			.build();
		MethodSpec afterChange = MethodSpec.methodBuilder("afterChange")
			.addModifiers(PRIVATE)
			.beginControlFlow("for (int i=0; i<observers.length; i++)")
			.addStatement("observers[i].afterChange(underlying, beforeValues[i])")
			.endControlFlow()
			.build();
		TypeSpec.Builder typeSpec = TypeSpec.classBuilder(getClassName(element))
			.addSuperinterface(underlyingType)
			.addModifiers(PUBLIC)
			.addField(FieldSpec.builder(underlyingType, "underlying")
				.addModifiers(PRIVATE)
				.build())
			.addField(FieldSpec.builder(observersType, "observers")
				.addModifiers(PRIVATE)
				.build())
			.addField(FieldSpec.builder(beforeValuesType, "beforeValues")
				.addModifiers(PRIVATE)
				.build())
			.addMethod(MethodSpec.constructorBuilder()
				.addModifiers(PUBLIC)
				.addParameter(underlyingType, "underlying")
				.addParameter(observersType, "observers")
				.varargs()
				.addStatement("this.underlying = underlying")
				.addStatement("this.observers = observers")
				.addStatement("this.beforeValues = new Object[observers.length]")
				.addStatement("this.afterChange()")
				.build())
			.addMethod(beforeChange)
			.addMethod(afterChange);
		for (Element e : elementUtils.getAllMembers((TypeElement)element)) {
			if (e.getKind() == ElementKind.METHOD && disjoint(e.getModifiers(), asList(FINAL, NATIVE))) {
				//TODO only NATIVE because javapoet overriding() can't handle it; report the bug!
				ExecutableElement ee = (ExecutableElement)e;
				MethodSpec method = getMethod(ee, beforeChange, afterChange).build();
				typeSpec.addMethod(method);
			}
		}
		write(element, typeSpec.build());
	}

	MethodSpec.Builder getMethod(ExecutableElement ee, MethodSpec beforeChange, MethodSpec afterChange) {
		StringBuilder paramString = new StringBuilder();
		for (VariableElement ve : ee.getParameters()) {
			if (paramString.length() > 0)
				paramString.append(", ");
			paramString.append(ve.getSimpleName().toString());
		}
		boolean isReturning = ee.getReturnType().getKind() != VOID;
		boolean isObservable = ee.getEnclosingElement().getAnnotation(Observable.class) != null;
		MethodSpec.Builder method = MethodSpec.overriding(ee);
		if (isObservable)
			method.addStatement("$N()", beforeChange);
		if (isReturning)
			method.addCode("$T returnValue = ", ee.getReturnType());
		method.addStatement("underlying.$L($L)", ee.getSimpleName(), paramString);
		if (isObservable)
			method.addStatement("$N()", afterChange);
		if (isReturning)
			method.addStatement("return returnValue");
		return method;
	}

        private void write(Element element, TypeSpec typeSpec) throws IOException {
                String packageName = elementUtils.getPackageOf(element).toString();
                JavaFileObject javaFileObject = filer.createSourceFile(packageName+"."+getClassName(element));
                Writer writer = javaFileObject.openWriter();
                JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
                javaFile.writeTo(writer);
                writer.close();
        }

        private String getClassName(Element element) {
		return element.getSimpleName().toString() + "Observable";
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return annotationTypesForClasses(Observable.class);
	}

	private Set<String> annotationTypesForClasses(Class<?>... classes) {
		return Arrays.stream(classes).map(c->c.getCanonicalName()).collect(toSet());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}
}
