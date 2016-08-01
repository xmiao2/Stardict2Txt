import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xm on 7/31/16.
 */
public class PinyinSeparator {

    public static final String vowels = "aāáǎàeēéěèiīíǐìoōóǒòuūúǔùǖǘǚǜü";

    public static final Pattern p1 = Pattern.compile("([" + vowels + "])([^" + vowels + "nr])");
    public static final Pattern p2 = Pattern.compile("(w)([csz]h)");
    public static final Pattern p3 = Pattern.compile("(n)([^" + vowels + "üvg])");
    public static final Pattern p4 = Pattern.compile("([" + vowels + "üv])([^" + vowels + "])([" + vowels + "üv])");
    public static final Pattern p5 = Pattern.compile("([" + vowels + "üv])(n)(g)([" + vowels + "üv])");
    public static final Pattern p6 = Pattern.compile("([gr])([^" + vowels + "])");
    public static final Pattern p7 = Pattern.compile("([^ēéěèe])(r)");

    public static final String s1 = "$1 $2";
    public static final String s2 = "$1 $2";
    public static final String s3 = "$1 $2";
    public static final String s4 = "$1 $2$3";
    public static final String s5 = "$1$2 $3$4";
    public static final String s6 = "$1 $2";
    public static final String s7 = "$1 $2";

    public static final String separate(String pinyin) {
        Matcher m1 = p1.matcher(pinyin);
        pinyin = m1.replaceAll(s1);

        Matcher m2 = p2.matcher(pinyin);
        pinyin = m2.replaceAll(s2);

        Matcher m3 = p3.matcher(pinyin);
        pinyin = m3.replaceAll(s3);

        Matcher m4 = p4.matcher(pinyin);
        pinyin = m4.replaceAll(s4);

        Matcher m5 = p5.matcher(pinyin);
        pinyin = m5.replaceAll(s5);

        Matcher m6 = p6.matcher(pinyin);
        pinyin = m6.replaceAll(s6);

        Matcher m7 = p7.matcher(pinyin);
        pinyin = m7.replaceAll(s7);

        return pinyin.trim();
    }
}
