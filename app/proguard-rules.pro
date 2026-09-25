# Add release-only keep rules when release shrinking is enabled.
# NewPipeExtractor uses Rhino for YouTube signature handling.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
