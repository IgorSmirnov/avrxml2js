package xml2js;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;


public class Main {
	public static String Convert(XMLNode Node, String n)
	{
		String r = "\n";
		r += n;
		r += Node.Name + ": ";
		XMLNode x = Node.FirstChild;
		if(x == null)
		{
			String v = Node.Value;
			if(v.charAt(0) == '[')
			{
				String t = "[";
				int y = 1;
				for(int c = 1, e = v.length(); c < e; c++)
				{
					char C = v.charAt(c);
					if(C == ':' || C == ']') 
					{
						if(t != "[") t += ",";
						if(c > y) t += "\"" + v.substring(y, c) + "\"";
						y = c + 1;
					}
				}
				r += t + "]";
			}
			else r += "\"" + v + "\"";
		} 
		else
		{
			r += "\n" + n + "{";
			String t = "";
			for(; x != null; x = x.Next) if(x.Name != null && (x.Value != null || x.FirstChild != null))
			{
				if(t != "") t += ",";				
				t += Convert(x, n + "\t");
			}
			r += t + "\n" + n + "}";
		}
		
		return r;
	}
	public static void main(String[] args) 
	{
		String[] Modules = "CORE,INTERRUPT_VECTOR,MEMORY,PACKAGE,POWER,IO_MODULE,V2".split(",");
		String Path = args[0];
		for(int a = 1; a < args.length; a++)try
		{
			String Part = args[a];
			File f = new File(Path + Part + ".xml");
			FileReader fr = new FileReader(f);
			long l = f.length();
			char[] b = new char[(int)l];
			fr.read(b);
			fr.close();

			XMLNode doc = new XMLNode();			
			doc.Load(new String(b));
			String r = "var " + Part + " = \n{";
			String t = "";
			for(int x = 0; x < Modules.length; x++)
			{
				XMLNode n = doc.get_Child(Modules[x]);
				if(n != null)
				{
					if(t != "") t += ",";
					t += Convert(n, "\t");
				}
			}
				
			r += t + "\n}";
			String fn = Path + Part + ".js";
			byte[] bb = r.getBytes("UTF-8");
			FileOutputStream fw = new FileOutputStream(fn);
			fw.write(bb);
			fw.close();

			System.out.println("Completed " + Part);
		} catch(XMLNode.XMLParseException e)
		{
			e.printStackTrace();
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
