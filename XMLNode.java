package xml2js;

public class XMLNode
{
	public class XMLParseException extends Exception
	{
		public String Message;
		int Pos;
		static final long serialVersionUID = 0;
		XMLParseException(String Message, int Pos)
		{
			this.Message = Message;
			this.Pos = Pos;
		}
	}
   	public class XMLAttribute
	{
        String Name, Value;
        XMLAttribute Next, Prev;
        XMLNode Parent;
    }
	private enum XMLMode {text, tag, cltag, param, equal, value}; 
   	
	XMLNode PI;
	String Name, Value;
	XMLNode Next, Prev, Parent;
	XMLNode FirstChild, LastChild;
	XMLAttribute FirstAttr, LastAttr;

	public XMLNode() {} 
	XMLAttribute get_Attribute(String Name)
	{
		for(XMLAttribute x = FirstAttr; x != null; x = x.Next)
			if(x.Name.equals(Name)) return x;
		return null;
	}
	String AttributeAsString(String Name)
	{
		for(XMLAttribute x = FirstAttr; x != null; x = x.Next)
			if(x.Name.equals(Name)) return x.Value;
		return null;
	}	
	int AttributeAsInt(String Name) throws XMLParseException
	{
		for(XMLAttribute x = FirstAttr; x != null; x = x.Next)
			if(x.Name.equals(Name)) return Integer.valueOf(x.Value);
		//return 0;
		throw new XMLParseException("Attribute '" + Name + "' of tag '" + this.Name + "' not found.", -1);
	}	
	XMLAttribute AppendAttribute(String Name)
	{
		XMLAttribute Result = get_Attribute(Name);
		if(Result != null) return Result;

		Result = new XMLAttribute();
		Result.Name = Name;
		Result.Parent = this;

		if(LastAttr != null) 
		{
			LastAttr.Next = Result;
			Result.Prev = LastAttr;
			LastAttr = Result;
		}
		else LastAttr = FirstAttr = Result;
		return Result;
	}
	static boolean IsLetter(char a) {return (a >= 'a' && a <= 'z') || (a >= 'A' && a <= 'Z') || a == '_' || a == '.' || a == '-';}
	static boolean IsLetterOrDigit(char a) {return (a >= 'a' && a <= 'z') || (a >= 'A' && a <= 'Z') || a == '_' || a == '.' || a == '-' || (a >= '0' && a <= '9');}
	static boolean IsDigit(char a) {return a >= '0' && a <= '9';}
	private int GetCommentEnd(char[] s, int Start) throws XMLParseException
	{
		int l = s.length;
		for(int x = Start; x < l; x++)
			if(s[x] == '-' && s[x + 1] == '-' && s[x + 2] == '>') return x;
		throw new XMLParseException("Comment not closed.", Start);
	}
	void loadXML(char[] XML, int Start) throws XMLParseException
	//  <Node Param="Value"> Text </Node> <
	// | |    |    ||      ||      ||
	// | |    |    |value  ||	   |cltag
	// | |    |    equal   |text   tag
	// | |    param        |   
	// | tag			   param
	// text				   
	{
		XMLMode Mode = XMLMode.text;
		int b = -1;
		char q = 0;
		Name = null;
		XMLNode Current = null;
		XMLAttribute Param = null;
		int Len = XML.length;
		for(int s = Start; s < Len; s++)
		{
			char c = XML[s];
			switch(Mode)
			{
			case text:
				if(c == '<')
				{
					Mode = XMLMode.tag;
					if(b != -1 && b < s) 
						Current.AppendTextFromParser(XML, b, s);
					b = -1;
				}
				else 
					if(c > ' ')
					{
						if(Current == null) throw new XMLParseException("Text not allowed here.", s);
						if(b < 0) b = s; // пїЅпїЅпїЅпїЅпїЅ trim пїЅпїЅпїЅпїЅпїЅпїЅ
					}
				continue;
			case cltag:
			case tag:
				if(b >= 0)
				{
					if((c <= ' ') || (c == '>') || (c == '/')) 
					{
						if(Mode == XMLMode.tag) // пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅ
						{
							String n = String.copyValueOf(XML, b, s - b);// ExtractNameFromParser(b, s);
							if(Current != null)
							{
								Current = Current.AppendChild(n);
							}
							else
							{
								Current = this;
								if(Name != null) throw new XMLParseException("Unexpected second root tag.", s);
								Name = n;
							}
							Mode = XMLMode.param;
							b = -1;
							s--;
						}
						else
						{
							if(Current == null) throw new XMLParseException("Unexpected end tag.", s);
							if(!Current.Name.equals(String.copyValueOf(XML, b, s - b)))
								throw new XMLParseException("End tag does not match start tag.", s);
							while(s < Len && XML[s] <= ' ') s++;
							if(XML[s] != '>') throw new XMLParseException("Expected '>'.", s);
							Current = Current.Parent;
							Mode = XMLMode.text;
							b = -1;//s + 1;
						}
						continue;
					} 
					else 
					{
						if(!IsLetterOrDigit(c)) throw new XMLParseException("Invalid character in name.", s);
						continue;
					}
				} 
				else 
				{
					if(c == '!') 
					{
						if((XML[s + 1] != '-') || (XML[s + 2] != '-')) throw new XMLParseException("Invalid name or comment.", s);
						b = GetCommentEnd(XML, s + 3);
						s = b + 2;
						b = -1;
						Mode = XMLMode.text;
						continue;
					}
					if(c <= ' ') throw new XMLParseException("White spaces not allowed here.", s);
					if(c == '/' && Mode == XMLMode.tag) { Mode = XMLMode.cltag; continue;}
					if(!IsLetter(c)) throw new XMLParseException("Invalid character in name.", s);
					b =	s;
					continue;
				}
			case param:
				if(b >= 0)
				{
					if(IsLetterOrDigit(c)) continue;
					Param = Current.AppendAttribute(String.copyValueOf(XML, b, s - b));
					b = -1;
					Mode = XMLMode.equal;
				}
				else
				{
					if(c <= ' ') continue;
					if(c == '/')
					{
						if(XML[s + 1] != '>') throw new XMLParseException("Expected '>' after '/'.", s);
						Current = Current.Parent;
						continue;
					}
					if(c == '>')
					{
						Mode = XMLMode.text;
						b = s + 1; // пїЅпїЅпїЅпїЅ -1, пїЅпїЅ пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ
						continue;
					}
					if(IsLetter(c)) b = s;
					else throw new XMLParseException("Invalid character in name.", s);
					continue;
				}
			case equal:
				if(c == '=') Mode = XMLMode.value;
				else
				if(c > ' ') throw new XMLParseException("'=' expected.", s);
				continue;
			case value:
				if(c == '"' || c == '\'')
				{
					if(b >= 0) 
					{
						if(q != c) continue;
						Param.Value = ExtractValueFromParser(XML, b, s);
						b = -1;
						Mode = XMLMode.param;
					}
					else 
					{
						q = c;
						b = s + 1;
					}
					continue;
				}
				if(b < 0 && c > ' ') throw new XMLParseException("Quote expected.", s);
			}
		}
		if(Mode != XMLMode.text || Current != null) throw new XMLParseException("Unexpected end of file.", -1);
		//if(!FirstChild) error(_T("File is empty."));
	};
	public void Delete(boolean Recurse)
	{
        Name = null;
        Value = null;
        XMLNode n;
		for(XMLNode x = FirstChild; x != null; x = n)
		{
			n = x.Next;
		    x.Parent = null;
		    x.Next = null;
		    x.Prev = null;
		    if(Recurse) x.Delete(Recurse);
        }
        XMLAttribute a;
		for(XMLAttribute x = FirstAttr; x != null; x = a)
		{
			a = x.Next;
		    x.Parent = null;
		    x.Next = null;
		    x.Prev = null;
        }
		
		SetParent(null);
	}
	public void Load(String Document) throws XMLParseException
	{
		PI = null;
		int s, l = Document.length();
		char[] XML = Document.toCharArray();
		for(s = 0; s < l && XML[s] <= ' '; s++);
		if(s >= l) throw new XMLParseException("Unexpected end of file.", s);
		if(XML[s] == '<' && XML[s + 1] == '?') 
		{
			PI = new XMLNode();
			s = PI.loadPI(XML);
		}
		/*if(PI != null)
		{
			String Encoding = PI.get_AttributeAsString("encoding");
			if(Encoding != null)
			{
				if(Encoding.equals("UTF-8"))
				{
					_char * R = (_char *)malloc((slen(s) + 1) * sizeof(_char));
					_char * r = R;
					for(const _char * x = s; *x; r++)
					{
						int Res;
						if((unsigned)*x < 0x80) 
							Res = (unsigned)*(x++);
						else
						if(*x & 0x20) 
						{ 
							Res = (unsigned short)((x[2] & 0x3F) | ((x[1] & 0x3F) << 6) | ((*x & 0x3F) << 12)); 
							x += 3;
						}
						else 
						{
							Res = (unsigned short)((x[1] & 0x3F) | ((*x & 0x3F) << 6)); 
							x += 2;
						}
						if(sizeof(_char) == 1)
						{
							if(Res < 0xC0) *r = Res;
							else
							if((Res >= 0x410) && (Res < 0x450)) *r = Res - 0x350;
							else *r = '?';
						} else *r = Res;
					}
					*r = 0;
					TCHAR * Result = XMLNode<_char>::loadXML(R, LastPos, Scheme);
					if(Result && LastPos && *LastPos) *LastPos += s - R;
					free(R);
					return Result;
				}
			}*/
			loadXML(XML, s);
		};
	/*public String WriteXML(int Level, String Tab)
	{
		String Destination = "";
			if(!Name) { Destination += sToXML<_char>(Destination, Value); return; } // пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ
		// пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
			_char * d = Destination;
			int tl;
			if(Tab)
			{
				*(d++) = 13;
				*(d++) = 10;
				tl = slen(Tab);
				for(int x = Level; x; x--)
				{
					memcpy(d, Tab, tl * sizeof(_char));
					d += tl;
				}
			}
			*(d++) = '<';
			int nl = slen(Name);
			memcpy(d, Name, nl * sizeof(_char)); d += nl;
		// пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
			for(XMLAttribute<_char> * a = FirstAttr; a; a = (XMLAttribute<_char> *) a.Next) 
			{
				*(d++) = ' ';
				int l = slen(a.Name);
				memcpy(d, a.Name, l * sizeof(_char)); d += l;
				*(d++) = '='; *(d++) = '"';
				d += sToXML(d, a.Value);
				*(d++) = '"';
			}

			if(FirstChild || Value)
			{
				*(d++) = '>';
				if(Value) d += sToXML(d, Value);
				// пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ
				for(XMLNode * x = FirstChild; x; x = (XMLNode<_char> *) x.Next) 
					x.WriteXML(d, Level + 1, Tab);
				if(Tab && FirstChild)
				{
					*(d++) = 13;
					*(d++) = 10;
					for(int x = Level; x; x--)
					{
						memcpy(d, Tab, tl * sizeof(_char));
						d += tl;
					}
				}
				*(d++) = '<';
				*(d++) = '/';
				memcpy(d, Name, nl * sizeof(_char)); d += nl;
				*(d++) = '>';
			}
			else
			{
				*(d++) = '/';
				*(d++) = '>';
			}
			Destination = d;
		};*/

	private XMLNode AppendTextFromParser(char[] Text, int Start, int Stop) throws XMLParseException
	{
		//for(Stop--; Text[Stop] <= ' ' && Stop >= Start; Stop--); Stop++; // пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
		char[] Result = new char[Stop - Start];
		XMLNode TextNode = this;
		if(LastChild != null) 
		{
			if(LastChild.Name != null) TextNode = AppendChild(null);
			else TextNode = LastChild;
		} 
		int r = 0, b = -1;
		for(int x = Start; x < Stop; x++)
		{
			char c = Text[x];
			if(c == '<') x = GetCommentEnd(Text, x + 4) + 2;
			else if(c == '&') b = x + 1;
			else if(b < 0) Result[r++] = c;
			else if(c == ';') 
			{
				String s = String.copyValueOf(Text, b, x - b);
				c = GetSymbolByName(s);
				if(c == 0) 
					throw new XMLParseException("Unknown character '&" + s + ";'", x);
				Result[r++] = c;
				b = -1;
			}
		}
		if(b >= 0) throw new XMLParseException("'&' without ';'.", b);
		String rs = String.copyValueOf(Result, 0, r);
		if(TextNode.Value != null) TextNode.Value += rs;
		else TextNode.Value = rs;
		return TextNode;
	}
	private char GetSymbolByName(String Name)
	{
		if(Name.equals("amp")) return '&';
		else if(Name.equals("lt")) return '<';
		else if(Name.equals("gt")) return '>';
		else if(Name.equals("apos")) return '\'';
		else if(Name.equals("quot")) return '"';
		else if(Name.charAt(0) == '#') 
			return '?';//(char) Integer.decode("0" + Name.substring(1)).byteValue();
		else return 0;
	}
	private String ExtractValueFromParser(char[] Text, int Start, int Stop) throws XMLParseException
	{
		char[] Result = new char[Stop - Start];
		int r = 0;
		char c;
		
		for(int x = Start; x < Stop; x++)
		{
			c = Text[x];
			if(c == '&') 
			{
				int t;
				for(t = ++x; x < Stop; x++)
					if(Text[x] == ';') break;
				if(x >= Stop) throw new XMLParseException("'&' without ';'", x);
				String i = String.copyValueOf(Text, t, x - t);
				c = GetSymbolByName(i);
				if(c == 0) throw new XMLParseException("Unknown character '&" + i + ";'", x);
			}
			Result[r++] = c;
		}
		return String.copyValueOf(Result, 0, r);
	};
	public XMLNode get_Child(String Name)
	{
		for(XMLNode x = FirstChild; x != null; x = x.Next) if(x.Name.equals(Name)) return x;
		return null;
	};
	public XMLNode AppendChild(String Name)
	{
		XMLNode Result = new XMLNode();
		Result.Name = Name;
		Result.Parent = this;

		if(LastChild != null) 
		{
			LastChild.Next = Result;
			Result.Prev = LastChild;
			LastChild = Result;
		}
		else LastChild = FirstChild = Result;
		return Result;
	}
	XMLNode AppendText(String Text)
	{
		XMLNode TextNode = this;
		if(LastChild != null) 
		{
			if(LastChild.Name != null) TextNode = AppendChild(null);
			else TextNode = LastChild;
		} 
		TextNode.Value += Text;
		return TextNode;
	}
	public void SetParent(XMLNode Parent)
	{
		if(this.Parent == Parent) return;
		if(this.Parent != null)
		{ // пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ
			if(Next != null) Next.Prev = Prev;
			else this.Parent.LastChild = Prev;
			if(Prev != null) Prev.Next = Next;
			else this.Parent.FirstChild = Next;
		}
		Next = null;
		Prev = null;
		if(Parent != null)
		{
			XMLNode Last = Parent.LastChild;
			if(Last != null)
			{
				Last.Next = this;
				Prev = Last;
			}
			else
			{
				Parent.FirstChild = this;
			}
			Parent.LastChild = this;
		}
		this.Parent = Parent;
	}

	/*/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
		int get_XMLSize(int Level, int TabLen)
		{
			if(!Name) return sXMLlen(Value); // пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ
			int Size = slen(Name);
			if(FirstChild || FValue) Size = Size * 2 + 5 + sXMLlen(Value); // Value
			else Size += 3; // 
			for(TNode<_char> * a = FirstAttr; a; a = a.Next)
				Size += slen(a.Name) + sXMLlen(a.Value) + 4; // пїЅпїЅпїЅпїЅпїЅпїЅпїЅ, =, пїЅпїЅпїЅпїЅпїЅпїЅ
		// пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
			for(XMLNode<_char> * x = FirstChild; x; x = (XMLNode<_char> *) x.Next) 
				Size += x.get_XMLSize(Level + TabLen, TabLen);
			if(TabLen)
			{
				if(FirstChild && FirstChild.FName) Size += Level + 2;
				Size += Level + 2;
			}
			return Size;
		};*/

	XMLNode(XMLNode Parent, String Name, String Value) 
	{
		this.Value = Value;
		this.Name = Name;
		SetParent(Parent);
	};	

	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ

	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅ пїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅ "\пїЅпїЅпїЅпїЅпїЅпїЅпїЅ\пїЅпїЅпїЅпїЅпїЅпїЅпїЅ!пїЅпїЅпїЅпїЅпїЅпїЅпїЅ"
	/*String ValueByPath(String Path)
	{
		int pos = 0;
		for(XMLNode x = this; x != null; )
		switch(Path.charAt(pos))
		{
		case '!':
			return x.get_AttributeAsString(Path.substring(pos + 1));
			case '/':
			case '\\':
				Path++;
				for(x = x.FirstChild; x != null; x = x.Next)
					if(const _char * y = x.Name)
					{
						const _char * p;
						for(p = Path; *y && (*p == *y); p++, y++);
						if(!*y && ((*p == '/') || (*p == '\\') || (*p == '!') || !*p))
						{
							Path = p;
							break;
						}
					}
				continue;
		case 0:
			return x;
		default: 
			return 0;
		}
		return null;
	}*/


		/*xml_virtual ~XMLNode(void)//! пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ.
		{
			XMLNode<_char> * n;
			for(XMLNode<_char> * x = FirstChild; x; x = n)
			{
				n = (XMLNode<_char> *)x.Next;
				x.Parent = 0;
				x.Next = 0;
				x.Prev = 0;
				delete x;
			}
			SetParent();
		}*/


		//TProcessingInstruction ProcessingInstruction;
	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ XML пїЅ пїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ.
		/*_char * get_XML(const _char * Tab = 0)
		{
			int Size = get_XMLSize(0, Tab ? slen(Tab) : 0) + 1;
			if(ProcessingInstruction) Size += ProcessingInstruction.get_XMLSize();
			_char * Result = (_char *)malloc(Size * sizeof(_char));
			_char * r = Result;
			if(ProcessingInstruction) ProcessingInstruction.WriteXML(r);
			WriteXML(r, 0, Tab);
			*r = 0;
			return Result;
		}*/
	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ XML пїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ.
	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ XML пїЅпїЅ пїЅпїЅпїЅпїЅпїЅ
		void CreatePI(String Encoding, String Version, String Name)
		{
			if(Name == null) Name = "xml";
			if(Version == null) Version = "1.0";
			PI = new XMLNode();
			PI.Name = Name;
			PI.AppendAttribute("version").Value = Version;
			if(Encoding != null) 
				PI.AppendAttribute("encoding").Value = Encoding;
		}

	//*************************************************************************************************
	//! пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ XML
	//*************************************************************************************************

	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ.
	/*	String WritePI()
		{
			if(!Name) return;
		// пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
			_char * d = Destination;
			*(d++) = '<';
			*(d++) = '?';
			int l = slen(Name);
			memcpy(d, Name, l * sizeof(_char)); d += l;
		// пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
			for(TNode<_char> * a = FirstAttr; a; a = a.Next) 
			{
				*(d++) = ' ';
				l = slen(a.Name);
				memcpy(d, a.Name, l * sizeof(_char)); d += l;
				*(d++) = '='; *(d++) = '"';
				d += sToXML(d, a.Value);
				*(d++) = '"';
			}
			*(d++) = '?';
			*(d++) = '>';
			Destination = d;
		}*/

	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
/*		int get_XMLSize(void)
		{
			if(!FName) return 0;
			int Size = slen(FName) + 4; // nt
			for(TNode<_char> * a = FirstAttr; a; a = a.Next) Size += slen(a.FName) + sXMLlen(a.FValue) + 4; // пїЅпїЅпїЅпїЅпїЅпїЅпїЅ, =, пїЅпїЅпїЅпїЅпїЅпїЅ
			return Size;
		};*/
	/// пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ. 
	/// пїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ 0 пїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ XML пїЅпїЅ пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
		int loadPI(char[] XML) throws XMLParseException
		{
			int s, l = XML.length;
			for(s = 0; XML[s] <= ' ' && s < l; s++);
			if((XML[s] != '<') || (XML[s + 1] != '?')) throw new XMLParseException("Missing '<?' on processing instruction.", s);
			s += 2;
			int b = s;
			if(IsLetter(XML[s])) for(; IsLetterOrDigit(XML[s]); s++);
			if(s == b || XML[s] > ' ') throw new XMLParseException("A name contains an invalid character or whitespace at start.", s);
			
			Name = String.copyValueOf(XML, b, s - b);
			XMLAttribute Param = null;
			for(s++; s < l; s++) if(XML[s] > ' ')
			{
				if(Param != null && XML[s] == '?' && XML[s + 1] == '>')
					return s + 2;
				b = s;
				if(IsLetter(XML[s])) for(; IsLetterOrDigit(XML[s]) && s < l; s++);
				if(s == b || (XML[s] > ' ') && (XML[s] != '=')) throw new XMLParseException("A name contains an invalid character or whitespace at start.", s);;
				Param = AppendAttribute(String.copyValueOf(XML, b, s - b));
				while(XML[s] <= ' ' && s < l) s++;
				if(XML[s++] != '=') throw new XMLParseException("Missing '=' between attribute and attribute value.", s);
				while(XML[s] <= ' ' && s < l) s++;
				char Q = XML[s++];
				if((Q != '\'') && (Q != '"')) throw new XMLParseException("Quote expected.", s);
				for(b = s; (XML[s] != Q) && s < l; s++);
				if(s >= l) 
					throw new XMLParseException("Unexpected end of file found.", s);
				Param.Value = String.copyValueOf(XML, b, s - b);
			}
			throw new XMLParseException("Unexpected end of file found.", s);
		}
}
