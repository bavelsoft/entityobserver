package com.bavelsoft.entityobserver;

//TODO optimize imports
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import static com.squareup.javapoet.MethodSpec.overriding;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import javax.lang.model.element.Modifier;
import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
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
import static javax.lang.model.type.TypeKind.VOID;
import javax.lang.model.type.ExecutableType;
import java.io.Writer;
import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import javax.tools.FileObject;
import static java.util.stream.Collectors.toSet;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import com.google.auto.service.AutoService;
import javax.inject.Named;


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
		String p = elementUtils.getPackageOf(element).toString();
		String c = element.getSimpleName().toString() + "Observable";
//TODO inner class support
		JavaFileObject o = filer.createSourceFile(p+"."+c);
		Writer w = o.openWriter();
		TypeName ti = TypeName.get(element.asType());
		TypeName at = ArrayTypeName.of(Observer.class);
		TypeName ab = ArrayTypeName.of(Object.class);
		TypeSpec.Builder t = TypeSpec.classBuilder(c)
			.addSuperinterface(ti)
			.addModifiers(Modifier.PUBLIC)
			.addField(FieldSpec.builder(ti, "underlying")
				.addModifiers(Modifier.PRIVATE)
				.build())
			.addField(FieldSpec.builder(at, "observers")
				.addModifiers(Modifier.PRIVATE)
				.build())
			.addField(FieldSpec.builder(ab, "beforeValues")
				.addModifiers(Modifier.PRIVATE)
				.build())
			.addMethod(MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ti, "underlying")
				.addParameter(at, "observers")
				.varargs()
				.addStatement("this.underlying = underlying")
				.addStatement("this.observers = observers")
				.addStatement("this.beforeValues = new Object[observers.length]")
				.build());
		for (Element e : ((TypeElement)element).getEnclosedElements()) {
			if (e.getKind() == ElementKind.METHOD) {
				ExecutableElement ee = (ExecutableElement)e;
				StringBuilder vb = new StringBuilder();
				for (VariableElement ve : ee.getParameters()) {
					if (vb.length() > 0)
						vb.append(", ");
					vb.append(ve.getSimpleName().toString());
				}
				boolean isVoid = ee.getReturnType().getKind() == VOID;
//TODO variable names not conflicting with parameter names
//TODO just delegate the methods in the (unannotated) superinterfaces
				MethodSpec.Builder m = overriding(ee)
					.addStatement("for (int i=0; i<observers.length; i++)\nbeforeValues[i] = observers[i].beforeChange(underlying)");
				if (isVoid)
					m.addStatement("underlying.$L($L)", ee.getSimpleName(), vb);
				else
					m.addStatement("$T returnValue = underlying.$L($L)", ee.getReturnType(), ee.getSimpleName(), vb);
				m.addStatement("for (int i=0; i<observers.length; i++)\nobservers[i].afterChange(underlying, beforeValues[i])");
				if (!isVoid)
					m.addStatement("return returnValue");
				t.addMethod(m.build());
			}
		}
		JavaFile f = JavaFile.builder(p, t.build()).build();
		f.writeTo(w);
		w.close();
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
