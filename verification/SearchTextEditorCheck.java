import com.miflix.native2.data.SearchTextEditor;
public final class SearchTextEditorCheck {
    static void check(boolean ok,String message) {if(!ok) throw new AssertionError(message);}
    public static void main(String[] args) {
        SearchTextEditor.Edit e=SearchTextEditor.insert("star wars",4,"ship");
        check(e.text.equals("starship wars") && e.cursor==8,"Insert at cursor");
        e=SearchTextEditor.backspace(e.text,e.cursor);check(e.text.equals("starshi wars") && e.cursor==7,"Delete before cursor");
        check(SearchTextEditor.move("abc",0,-1)==0 && SearchTextEditor.move("abc",3,1)==3,"Cursor boundaries");
        String emoji="A🔥B";
        check(SearchTextEditor.move(emoji,1,1)==3,"Move over whole emoji");
        check(SearchTextEditor.move(emoji,3,-1)==1,"Move back over whole emoji");
        e=SearchTextEditor.backspace(emoji,3);check(e.text.equals("AB") && e.cursor==1,"Delete whole emoji");
        e=SearchTextEditor.insert(emoji,2,"X");check(e.text.equals("AX🔥B") && e.cursor==2,"Repair mid-surrogate cursor");
        e=SearchTextEditor.replace("🔥".repeat(120));check(e.text.codePointCount(0,e.text.length())==100 && e.cursor==200,"Unicode length limit");
        e=SearchTextEditor.insert(e.text,e.cursor,"Z");check(e.text.codePointCount(0,e.text.length())==100,"Full field refuses overflow");
        e=SearchTextEditor.replace(null);check(e.text.isEmpty() && e.cursor==0,"Clear field");
        System.out.println("PASS: mid-title insertion/deletion, cursor boundaries, Unicode movement/deletion and length limit");
    }
}
