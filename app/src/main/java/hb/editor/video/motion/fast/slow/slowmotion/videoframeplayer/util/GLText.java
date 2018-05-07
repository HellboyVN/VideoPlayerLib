package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

public class GLText {

    public static final String FRAGMENT_SHADER_BW = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n  vec4 color = texture2D(sTexture, vTextureCoord);\n   float intensity = color.r*0.299 + color.g*0.587 + 0.114 *color.b;   gl_FragColor = vec4(intensity, intensity, intensity,1.0);}\n";

    public static final String EDGE = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float; \n" +
            "uniform samplerExternalOES u_Texture; \n" +
            "varying vec2 vTextureCoord; \n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec3 irgb = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "    float ResS = 720.;\n" +
            "    float ResT = 720.;\n" +
            "\n" +
            "    vec2 stp0 = vec2(1./ResS, 0.);\n" +
            "    vec2 st0p = vec2(0., 1./ResT);\n" +
            "    vec2 stpp = vec2(1./ResS, 1./ResT);\n" +
            "    vec2 stpm = vec2(1./ResS, -1./ResT);\n" +
            "\n" +
            "    const vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
            "    float i00 = dot(texture2D(u_Texture, vTextureCoord).rgb, W);\n" +
            "    float im1m1 = dot(texture2D(u_Texture, vTextureCoord-stpp).rgb, W);\n" +
            "    float ip1p1 = dot(texture2D(u_Texture, vTextureCoord+stpp).rgb, W);\n" +
            "    float im1p1 = dot(texture2D(u_Texture, vTextureCoord-stpm).rgb, W);\n" +
            "    float ip1m1 = dot(texture2D(u_Texture, vTextureCoord+stpm).rgb, W);\n" +
            "    float im10 = dot(texture2D(u_Texture, vTextureCoord-stp0).rgb, W);\n" +
            "    float ip10 = dot(texture2D(u_Texture, vTextureCoord+stp0).rgb, W);\n" +
            "    float i0m1 = dot(texture2D(u_Texture, vTextureCoord-st0p).rgb, W);\n" +
            "    float i0p1 = dot(texture2D(u_Texture, vTextureCoord+st0p).rgb, W);\n" +
            "    float h = -1.*im1p1 - 2.*i0p1 - 1.*ip1p1 + 1.*im1m1 + 2.*i0m1 + 1.*ip1m1;\n" +
            "    float v = -1.*im1m1 - 2.*im10 - 1.*im1p1 + 1.*ip1m1 + 2.*ip10 + 1.*ip1p1;\n" +
            "\n" +
            "    float mag = length(vec2(h, v));\n" +
            "    vec3 target = vec3(mag, mag, mag);\n" +
            "    gl_FragColor = vec4(mix(irgb, target, 1.0),1.);\n" +
            "}";

    public static final String BLUR = "#extension GL_OES_EGL_image_external : require\n" + "precision mediump float; \n" +
            "uniform samplerExternalOES u_Texture;\n" +
            "varying vec2 vTextureCoord; \n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec3 irgb = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "    float ResS = 720.;\n" +
            "    float ResT = 720.;\n" +
            "\n" +
            "    vec2 stp0 = vec2(1./ResS, 0.);\n" +
            "    vec2 st0p = vec2(0., 1./ResT);\n" +
            "    vec2 stpp = vec2(1./ResS, 1./ResT);\n" +
            "    vec2 stpm = vec2(1./ResS, -1./ResT);\n" +
            "\n" +
            "    vec3 i00 = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "    vec3 im1m1 = texture2D(u_Texture, vTextureCoord-stpp).rgb;\n" +
            "    vec3 ip1p1 = texture2D(u_Texture, vTextureCoord+stpp).rgb;\n" +
            "    vec3 im1p1 = texture2D(u_Texture, vTextureCoord-stpm).rgb;\n" +
            "    vec3 ip1m1 = texture2D(u_Texture, vTextureCoord+stpm).rgb;\n" +
            "    vec3 im10 = texture2D(u_Texture, vTextureCoord-stp0).rgb;\n" +
            "    vec3 ip10 = texture2D(u_Texture, vTextureCoord+stp0).rgb;\n" +
            "    vec3 i0m1 = texture2D(u_Texture, vTextureCoord-st0p).rgb;\n" +
            "    vec3 i0p1 = texture2D(u_Texture, vTextureCoord+st0p).rgb;\n" +
            "\n" +
            "    vec3 target = vec3(0., 0., 0.);\n" +
            "    target += 1.*(im1m1+ip1m1+ip1p1+im1p1); \n" +
            "    target += 2.*(im10+ip10+i0p1);\n" +
            "    target += 4.*(i00);\n" +
            "    target /= 16.;\n" +
            "    gl_FragColor = vec4(target, 1.);\n" +
            "}";

    public static final String EMBOSS = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float; \n" +
            "uniform samplerExternalOES u_Texture; \n" +
            "varying vec2 vTextureCoord; \n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec3 irgb = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "    float ResS = 360.;\n" +
            "    float ResT = 360.;\n" +
            "\n" +
            "    vec2 stp0 = vec2(1./ResS, 0.);\n" +
            "    vec2 stpp = vec2(1./ResS, 1./ResT);\n" +
            "    vec3 c00 = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "    vec3 cp1p1 = texture2D(u_Texture, vTextureCoord + stpp).rgb;\n" +
            "\n" +
            "    vec3 diffs = c00 - cp1p1;\n" +
            "    float max = diffs.r;\n" +
            "    if(abs(diffs.g)>abs(max)) max = diffs.g;\n" +
            "    if(abs(diffs.b)>abs(max)) max = diffs.b;\n" +
            "\n" +
            "    float gray = clamp(max + .5, 0., 1.);\n" +
            "    vec3 color = vec3(gray, gray, gray);\n" +
            "\n" +
            "    gl_FragColor = vec4(mix(color,c00, 0.1), 1.);\n" +
            "}";

    public static final String CONTRAST = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;\n" +
            "uniform samplerExternalOES u_Texture;\n" +
            "varying vec2 vTextureCoord;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    float T = 2.0;\n" +
            "    vec2 st = vTextureCoord.st;\n" +
            "    vec3 irgb = texture2D(u_Texture, st).rgb;\n" +
            "    vec3 target = vec3(0.5, 0.5, 0.5);\n" +
            "    gl_FragColor = vec4(mix(target, irgb, T), 1.);\n" +
            "}";

    public static final String FLIP_VT = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;       \t// Set the default precision to medium. We don't need as high of a \n" +
            "\t\t\t\t\t\t\t\t// precision in the fragment shader.\n" +
            "\n" +
            "uniform samplerExternalOES u_Texture;    // The input texture.\n" +
            "  \n" +
            "varying vec2 vTextureCoord;   // Interpolated texture coordinate per fragment\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\tvec2 st = vTextureCoord.st;\n" +
            "\tst.s = 1. - st.s;\n" +
            "\tst.t = 1. - st.t;\n" +
            "\t\n" +
            "\tvec3 irgb = texture2D(u_Texture, st).rgb;\n" +
            "\tgl_FragColor = vec4(irgb, 1.);\n" +
            "}";
    public static final String FLIP_HZ = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;       \t// Set the default precision to medium. We don't need as high of a \n" +
            "\t\t\t\t\t\t\t\t// precision in the fragment shader.\n" +
            "\n" +
            "uniform samplerExternalOES u_Texture;    // The input texture.\n" +
            "  \n" +
            "varying vec2 vTextureCoord;   // Interpolated texture coordinate per fragment\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\tvec2 st = vTextureCoord.st;\n" +
            "\tst.s = 1. - st.s;\n" +
            "\t\n" +
            "\tvec3 irgb = texture2D(u_Texture, st).rgb;\n" +
            "\tgl_FragColor = vec4(irgb, 1.);\n" +
            "}";

    public static final String HUESHIFT = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;       \t// Set the default precision to medium. We don't need as high of a \n" +
            "\t\t\t\t\t\t\t\t// precision in the fragment shader.\n" +
            "\n" +
            "uniform samplerExternalOES u_Texture;    // The input texture.\n" +
            "  \n" +
            "varying vec2 vTextureCoord;   // Interpolated texture coordinate per fragment\n" +
            "\n" +
            "const float shift = 90.;\n" +
            "\n" +
            "vec3 convertRGB2HSV(vec3 rgbcolor){\n" +
            "\tfloat h, s, v;\n" +
            "\t\n" +
            "\tfloat r = rgbcolor.r;\n" +
            "\tfloat g = rgbcolor.g;\n" +
            "\tfloat b = rgbcolor.b;\n" +
            "\tv = max(r, max(g, b));\n" +
            "\tfloat maxval = v;\n" +
            "\tfloat minval = min(r, min(g,b));\n" +
            "\t\n" +
            "\tif(maxval == 0.)\n" +
            "\t\ts = 0.0;\n" +
            "\telse\n" +
            "\t\ts = (maxval - minval)/maxval;\n" +
            "\t\t\n" +
            "\tif(s == 0.)\n" +
            "\t\th = 0.; \n" +
            "\telse{\n" +
            "\t\tfloat delta = maxval - minval;\n" +
            "\t\t\n" +
            "\t\tif(r == maxval)\n" +
            "\t\t\th = (g-b)/delta;\n" +
            "\t\telse\n" +
            "\t\t\tif(g == maxval)\n" +
            "\t\t\t\th = 2.0 + (b-r)/delta;\n" +
            "\t\t\telse\n" +
            "\t\t\t\tif(b == maxval)\n" +
            "\t\t\t\t\th = 4.0 + (r-g)/delta;\n" +
            "\t\t\n" +
            "\t\th*= 60.;\n" +
            "\t\tif( h < 0.0)\n" +
            "\t\t\th += 360.;\t\n" +
            "\t}\n" +
            "\t\n" +
            "\treturn vec3(h, s, v);\n" +
            "}\n" +
            "\n" +
            "vec3 convertHSV2RGB(vec3 hsvcolor)\n" +
            "{\n" +
            "\tfloat h = hsvcolor.x;\n" +
            "\tfloat s = hsvcolor.y;\n" +
            "\tfloat v = hsvcolor.z;\n" +
            "\t\n" +
            "\tif(s == 0.0)\n" +
            "\t{\n" +
            "\t\treturn vec3(v,v,v);\n" +
            "\t}\n" +
            "\t\n" +
            "\telse{\n" +
            "\t\tif(h > 360.0) h = 360.0;\n" +
            "\t\tif(h < 0.0) h = 0.0;\n" +
            "\t\t\n" +
            "\t\th /= shift;\n" +
            "\t\tint k = int(h);\n" +
            "\t\tfloat f = h - float(k);\n" +
            "\t\tfloat p = v*(1.0-s);\n" +
            "\t\tfloat q = v*(1.0-(s*f));\n" +
            "\t\tfloat t = v*(1.0-(s*(1.0-f)));\n" +
            "\t\t\n" +
            "\t\tvec3 target;\n" +
            "\t\tif(k==0) target = vec3(v,t,p);\n" +
            "\t\tif(k==1) target = vec3(q,v,p);\n" +
            "\t\tif(k==2) target = vec3(p,v,t);\n" +
            "\t\tif(k==3) target = vec3(p,q,v);\n" +
            "\t\tif(k==4) target = vec3(t,p,v);\n" +
            "\t\tif(k==5) target = vec3(v,p,q);\n" +
            "\t\t\n" +
            "\t\treturn target;\n" +
            "\t}\n" +
            "}\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\t//angle of the hue shifting\n" +
            "\tfloat T = 70.0;\n" +
            "\t\n" +
            "\tvec3 irgb = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "\tvec3 ihsv = convertRGB2HSV(irgb);\n" +
            "\tihsv.x += T;\n" +
            "\tif(ihsv.x > 360.) ihsv.x -= 360.;\n" +
            "\tif(ihsv.x < 0.) ihsv.x += 360.;\n" +
            "\tirgb = convertHSV2RGB(ihsv);\n" +
            "\tgl_FragColor = vec4(irgb, 1.);\n" +
            "}";

    public static final String LUMINANCE = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;       \t// Set the default precision to medium. We don't need as high of a \n" +
            "\t\t\t\t\t\t\t\t// precision in the fragment shader.\n" +
            "\n" +
            "uniform samplerExternalOES u_Texture;    // The input texture.\n" +
            "  \n" +
            "varying vec2 vTextureCoord;   // Interpolated texture coordinate per fragment.\n" +
            "  \n" +
            "// The entry point for our fragment shader.\n" +
            "void main()                    \t\t\n" +
            "{ \n" +
            "\t//wegith constant\n" +
            "\tconst vec3 W = vec3(0.2125, 0.1754, 0.0721);\n" +
            "\t\n" +
            "\t//get rgb color from texture\n" +
            "\tvec3 irgb = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "\t\n" +
            "\t//get luminance\n" +
            "\tfloat luminance = dot(irgb, W);\n" +
            "\t\n" +
            "\t//output the pixel\n" +
            "\tgl_FragColor = vec4(luminance, luminance, luminance, 1.);                                 \t\t\n" +
            "}               ";

    public static final String NEGATIVE = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;\n" +
            "uniform samplerExternalOES u_Texture;\n" +
            "varying vec2 vTextureCoord; \n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    float T = 1.0; \n" +
            "    vec2 st = vTextureCoord.st;\n" +
            "    vec3 irgb = texture2D(u_Texture, st).rgb;\n" +
            "    vec3 neg = vec3(1., 1., 1.)-irgb;\n" +
            "    gl_FragColor = vec4(mix(irgb,neg, T), 1.);\n" +
            "}";

    public static final String TOON = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;       \t// Set the default precision to medium. We don't need as high of a \n" +
            "\t\t\t\t\t\t\t\t// precision in the fragment shader.\n" +
            "\n" +
            "uniform samplerExternalOES u_Texture;    // The input texture.\n" +
            "  \n" +
            "varying vec2 vTextureCoord;   // Interpolated texture coordinate per fragment\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\tfloat ResS = 720.;\n" +
            "\tfloat ResT = 720.;\n" +
            "\tfloat MagTol = .5;\n" +
            "\tfloat Quantize = 10.;\n" +
            "\t\n" +
            "\tvec3 irgb = texture2D(u_Texture, vTextureCoord).rgb;\n" +
            "\tvec2 stp0 = vec2(1./ResS, 0.);\n" +
            "\tvec2 st0p = vec2(0., 1./ResT);\n" +
            "\tvec2 stpp = vec2(1./ResS, 1./ResT);\n" +
            "\tvec2 stpm = vec2(1./ResS, -1./ResT);\n" +
            "\t\n" +
            "\tconst vec3 W = vec3(0.2125, 0.7154, 0.0721);\n" +
            "\tfloat i00 = \tdot(texture2D(u_Texture, vTextureCoord).rgb, W);\n" +
            "\tfloat im1m1 =\tdot(texture2D(u_Texture, vTextureCoord-stpp).rgb, W);\n" +
            "\tfloat ip1p1 = \tdot(texture2D(u_Texture, vTextureCoord+stpp).rgb, W);\n" +
            "\tfloat im1p1 = \tdot(texture2D(u_Texture, vTextureCoord-stpm).rgb, W);\n" +
            "\tfloat ip1m1 = \tdot(texture2D(u_Texture, vTextureCoord+stpm).rgb, W);\n" +
            "\tfloat im10 = \tdot(texture2D(u_Texture, vTextureCoord-stp0).rgb, W);\n" +
            "\tfloat ip10 = \tdot(texture2D(u_Texture, vTextureCoord+stp0).rgb, W);\n" +
            "\tfloat i0m1 = \tdot(texture2D(u_Texture, vTextureCoord-st0p).rgb, W);\n" +
            "\tfloat i0p1 = \tdot(texture2D(u_Texture, vTextureCoord+st0p).rgb, W);\n" +
            "\t\n" +
            "\t//H and V sobel filters\n" +
            "\tfloat h = -1.*im1p1 - 2.*i0p1 - 1.*ip1p1 + 1.*im1m1 + 2.*i0m1 + 1.*ip1m1;\n" +
            "\tfloat v = -1.*im1m1 - 2.*im10 - 1.*im1p1 + 1.*ip1m1 + 2.*ip10 + 1.*ip1p1;\n" +
            "\tfloat mag = length(vec2(h, v));\n" +
            "\t\n" +
            "\tif(mag > MagTol){\n" +
            "\t\tgl_FragColor = vec4(0., 0., 0., 1.);\n" +
            "\t}else{\n" +
            "\t\tirgb.rgb *= Quantize;\n" +
            "\t\tirgb.rgb += vec3(.5,.5,.5);\n" +
            "\t\tivec3 intrgb = ivec3(irgb.rgb);\n" +
            "\t\tirgb.rgb = vec3(intrgb)/Quantize;\n" +
            "\t\tgl_FragColor = vec4(irgb, 1.);\n" +
            "\t}\n" +
            "}";

    public static final String TWIRL = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;       \t// Set the default precision to medium. We don't need as high of a \n" +
            "\t\t\t\t\t\t\t\t// precision in the fragment shader.\n" +
            "\n" +
            "uniform samplerExternalOES u_Texture;    // The input texture.\n" +
            "  \n" +
            "varying vec2 vTextureCoord;   // Interpolated texture coordinate per fragment\n" +
            "\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\tfloat Res = 720.;\n" +
            "\tfloat D = -45.;\n" +
            "\tfloat R = 0.3;\n" +
            "\t\n" +
            "\tvec2 st = vTextureCoord.st;\n" +
            "\tfloat Radius = Res * R;\n" +
            "\tvec2 xy = Res * st;\n" +
            "\t\n" +
            "\tvec2 dxy = xy - Res/2.;\n" +
            "\tfloat r = length(dxy);\n" +
            "\tfloat beta = atan(dxy.y, dxy.x) + radians(D)*(Radius - r)/Radius;\n" +
            "\t\n" +
            "\tvec2 xy1 = xy;\n" +
            "\tif(r <= Radius)\n" +
            "\t{\n" +
            "\t\txy1.s = Res/2. + r*vec2(cos(beta)).s;\n" +
            "\t\txy1.t = Res/2. + r*vec2(sin(beta)).t;\n" +
            "\t}\n" +
            "\tst = xy1/Res;\n" +
            "\t\n" +
            "\tvec3 irgb = texture2D(u_Texture, st).rgb;\n" +
            "\tgl_FragColor = vec4(irgb, 1.);\n" +
            "}";

    public static final String WARP = "#extension GL_OES_EGL_image_external : require\n" +"precision mediump float;       \t// Set the default precision to medium. We don't need as high of a \n" +
            "\t\t\t\t\t\t\t\t// precision in the fragment shader.\n" +
            "\n" +
            "uniform samplerExternalOES u_Texture;    // The input texture.\n" +
            "  \n" +
            "varying vec2 vTextureCoord;   // Interpolated texture coordinate per fragment\n" +
            "const float PI = 3.14159265;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "\tfloat T = 0.2;\n" +
            "\tvec2 st = vTextureCoord.st;\n" +
            "\tvec2 xy = st;\n" +
            "\txy = 2.*xy - 1.;\n" +
            "\txy += T*sin(PI*xy);\n" +
            "\tst = (xy + 1.)/2.;\n" +
            "\t\n" +
            "\tvec3 irgb = texture2D(u_Texture, st).rgb;\n" +
            "\tgl_FragColor = vec4(irgb, 1.);\n" +
            "}";


}
