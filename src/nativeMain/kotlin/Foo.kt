import jni.*
import kotlinx.cinterop.*

fun main(vararg args: String) {
    require(args.size == 3) { "Needs classpath + 2 parameters " }
    requireNotNull(args[2].toIntOrNull())

    val classPath = "-Djava.class.path=${args[0]}"
    println(classPath)
    // val libPath = "-Djava.library.path=${args[1]}"
    val jvm = cValuesOf<JavaVMVar>()
    try {
        memScoped {
            val vmArgs = alloc<JavaVMInitArgs>()
            vmArgs.version = JNI_VERSION_10
            vmArgs.nOptions = 1
            val options = allocArray<JavaVMOption>(vmArgs.nOptions)
            options[0].optionString = classPath.cstr.ptr
            //options[1].optionString = libPath.cstr.ptr

            vmArgs.options = options
            val env = alloc<JNIEnvVar>().ptr

            println("CREATE JVM")
            val resultCreateJvm = JNI_CreateJavaVM(jvm, env.reinterpret(), vmArgs.ptr)
            println("CHECK JVM")
            require(resultCreateJvm == JNI_OK) {
                "JNI_CreateJavaVM failed"
            }
            println("JVM CREATED")

            val jcls = env.findClass("sample/MainKt")
            println("GOT MainKt")
            val jclEntry = env.getStaticMethod(jcls, "cobolEntry", "(Lsample/Linking;)V")

            val optionsClass = env.findClass("sample/Linking")
            val optionsObject = env.newObject(
                optionsClass,
                env.getMethod(optionsClass, "<init>", "(Ljava/lang/String;I)V"),
                {
                    l = env.newUtfString(args[1])
                },
                {
                    i = args[2].toInt()
                }
            )
            println("CREATE Main.Linking")

            env.callStaticVoidMethod(jcls, jclEntry, {
                l = optionsObject
            })
            println("CALLED cobolentry")

            val changedI = env.callIntMethod(optionsObject, env.getMethod(optionsClass, "getI", "()I"))
            val changedS =
                env.callObjectMethodA(optionsObject, env.getMethod(optionsClass, "getS", "()Ljava/lang/String;"))

            println("$changedI, ${env.pointed.pointed!!.GetStringChars!!(env, changedS, null)!!.toKString()}")
        }
    } finally {
        println("SHUTDOWN JVM")
        if (jvm.size != 0) {
            memScoped {
                jvm.ptr[0]!!.pointed.pointed!!.DestroyJavaVM!!(jvm.ptr[0])
            }
        }
        println("FINISHED")
    }
}

private fun CPointer<JNIEnvVar>.newUtfString(string: String): jstring {
    return memScoped {
        pointed.pointed!!.NewStringUTF!!(this@newUtfString, string.cstr.ptr)!!
    }
}

private fun CPointer<JNIEnvVar>.callIntMethod(
    jobject: jobject,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
): jint {
    val f = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    return pointed.pointed!!.CallIntMethodA!!(this@callIntMethod, jobject, method, f)
}

private fun CPointer<JNIEnvVar>.callObjectMethodA(
    jobject: jobject,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
): jobject {
    val f = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    return pointed.pointed!!.CallObjectMethodA!!(this@callObjectMethodA, jobject, method, f)!!
}

private fun CPointer<JNIEnvVar>.callStaticVoidMethod(
    jClass: jclass,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
) {
    val args = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    pointed.pointed!!.CallStaticVoidMethodA!!(this@callStaticVoidMethod, jClass, method, args)
}

private fun CPointer<JNIEnvVar>.getField(jobject: jobject, name: String, type: String): jfieldID {
    return memScoped {
        pointed.pointed!!.GetFieldID!!(this@getField, jobject, name.cstr.ptr, type.cstr.ptr)!!
    }
}

private fun CPointer<JNIEnvVar>.getMethod(jClass: jclass, name: String, parameter: String): jmethodID {
    return memScoped {
        pointed.pointed!!.GetMethodID!!(this@getMethod, jClass, name.cstr.ptr, parameter.cstr.ptr)!!
    }
}

private fun CPointer<JNIEnvVar>.getStaticMethod(jClass: jclass, name: String, parameter: String): jmethodID =
    memScoped {
        pointed.pointed!!.GetStaticMethodID!!(this@getStaticMethod, jClass, name.cstr.ptr, parameter.cstr.ptr)!!
    }

private fun CPointer<JNIEnvVar>.newObject(
    jClass: jclass,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
): jobject {
    val args = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    return pointed.pointed!!.NewObjectA!!(this@newObject, jClass, method, args)!!
}

private fun CPointer<JNIEnvVar>.findClass(className: String): jclass = memScoped {
    val p = requireNotNull(pointed.pointed) { "ENV ERROR in findClass: pointed.pointed was null" }
    val FindClass = requireNotNull(p.FindClass) { "p.FindClass was null" }
    val jlass = FindClass(this@findClass, className.cstr.ptr)
    requireNotNull(jlass) { "jclass for $className was not found" }
}

/*
LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/Users/philipwedemann/Library/Java/JavaVirtualMachines/azul-17.0.5/Contents/Home/lib/server build/bin/macosArm64/debugExecutable/jniTest.kexe /Users/philipwedemann/Downloads/jniTest/build/classes/kotlin/jvm/main:/Users/philipwedemann/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk8/1.8.0-RC2/c7080e0e0c608235bf07d8542dd2b2589bbb8881/kotlin-stdlib-jdk8-1.8.0-RC2.jar:/Users/philipwedemann/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk7/1.8.0-RC2/bed3d73e81d474ba2119ae203bdd15e96d7cf0bc/kotlin-stdlib-jdk7-1.8.0-RC2.jar:/Users/philipwedemann/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.8.0-RC2/89303520e71f5a7eda0b21ce8a3bd9f0154921bb/kotlin-stdlib-1.8.0-RC2.jar:/Users/philipwedemann/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar Hello 2 
*/
