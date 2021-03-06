/*
Storybook: Open Source software for novelists and authors.
Copyright (C) 2008 - 2012 Martin Mustun

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package storybook.model.hbn.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.apache.commons.text.util.StringEscapeUtils;

import org.hibernate.proxy.HibernateProxyHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import storybook.toolkit.html.HtmlUtil;

public abstract class AbstractEntity implements Serializable {

	private static Long transientIdCounter = 1L;

	protected Long id = -1L;
	private Long transientId = -1L;
	// TODO add maj field in each hbm.xml
	private Timestamp maj;

	public AbstractEntity() {
		super();
		transientId = transientIdCounter++;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isTransient() {
		return id.intValue() == -1L;
	}

	public Long getTransientId() {
		return transientId;
	}

	public String get() {
		return "";
	}
	
	public void setMaj() {
		maj=new Timestamp(Calendar.getInstance().getTime().getTime());
	}
	
	public Timestamp getMaj() {
		return(maj);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		if (isTransient()) {
			hash = hash * 31
					+ (transientId != null ? transientId.hashCode() : 0);
		}
		// TODO add maj to each hbm.xml
		//hash = hash * 31 + (maj != null ? maj.hashCode() : 0);
		return hash;
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		// hibernate object may have class names like Person_$$_javassist_11
		Class<?> cl1 = HibernateProxyHelper.getClassWithoutInitializingProxy(this);
		Class<?> cl2 = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
		if (cl1 != cl2) {
			return false;
		}
		AbstractEntity test = (AbstractEntity) obj;
		// for test use getter here since hibernate may need to load it first
		if (isTransient()) {
			return transientId.equals(test.getTransientId());
		}
		return id.equals(test.getId());
	}

	public static boolean equalsObjectNullValue(Object o1, Object o2) {
		if (o1 != null && o2 == null) {
			return false;
		}
		if (o1 == null && o2 != null) {
			return false;
		}
		if (o1 != null) {
			if (o2 == null) {
				return false;
			}
			return o1.equals(o2);
		}
		return true;
	}

	public static boolean equalsStringNullValue(String s1, String s2) {
		if (s1 != null && s2 == null) {
			return false;
		}
		if (s1 == null && s2 != null) {
			return false;
		}
		if (s1 != null) {
			if (s2 == null) {
				return false;
			}
			String st1 = HtmlUtil.htmlToText(s1);
			String st2 = HtmlUtil.htmlToText(s2);
			return st1.equals(st2);
		}
		return true;
	}

	public static boolean equalsIntegerNullValue(Integer n1, Integer n2) {
		if (n1 != null && n2 == null) {
			return false;
		}
		if (n1 == null && n2 != null) {
			return false;
		}
		if (n1 != null) {
			if (n2 == null) {
				return false;
			}
			return n1.equals(n2);
		}
		return true;
	}

	public static boolean equalsLongNullValue(Long l1, Long l2) {
		if (l1 != null && l2 == null) {
			return false;
		}
		if (l1 == null && l2 != null) {
			return false;
		}
		if (l1 != null) {
			if (l2 == null) {
				return false;
			}
			return l1.equals(l2);
		}
		return true;
	}

	public static boolean equalsDateNullValue(Date d1, Date d2) {
		if (d1 != null && d2 == null) {
			return false;
		}
		if (d1 == null && d2 != null) {
			return false;
		}
		if (d1 != null) {
			if (d2 == null) {
				return false;
			}
			return (d1.compareTo(d2) == 0);
		}
		return true;
	}

	public static boolean equalsTimestampNullValue(Timestamp ts1, Timestamp ts2) {
		if (ts1 != null && ts2 == null) {
			return false;
		}
		if (ts1 == null && ts2 != null) {
			return false;
		}
		if (ts1 != null) {
			if (ts2 == null) {
				return false;
			}
			return (ts1.compareTo(ts2) == 0);
		}
		return true;
	}

	public static boolean equalsListNullValue(
			List<? extends AbstractEntity> li1,
			List<? extends AbstractEntity> li2) {
		if (li1 == null && li2 == null) {
			return true;
		}
		if (li1 == null || li2 == null) {
			return false;
		}
		if (li1.isEmpty() && li2.isEmpty()) {
			return true;
		}
		if (li1.size() != li2.size()) {
			return false;
		}
		List<Long> ids1 = new ArrayList<>();
		for (AbstractEntity e : li1) {
			ids1.add(e.getId());
		}
		List<Long> ids2 = new ArrayList<>();
		for (AbstractEntity e : li2) {
			ids2.add(e.getId());
		}
		ids1.removeAll(ids2);
		return ids1.isEmpty();
	}

	public static int getListHashCode(List<?> list) {
		int hash = 31;
		for (Object o : list) {
			AbstractEntity e = (AbstractEntity) o;
			hash = hash * 31 + (e.getId() != null ? e.getId().hashCode() : 0);
		}
		return hash;
	}

	public static boolean equalsBooleanNullValue(Boolean b1, Boolean b2) {
		if (b1 != null && b2 == null) {
			return false;
		}
		if (b1 == null && b2 != null) {
			return false;
		}
		if (b1 != null) {
			if (b2 == null) {
				return false;
			}
			return b1.equals(b2);
		}
		return true;
	}

	public String getAbbr() {
		return toString();
	}

	public Icon getImageIcon() {
		return new ImageIcon();
	}
	
	/*
	 * export Entity to CSV format
	 * all data would be exported
	 */
	public String toCsv(String quoteStart, String quoteEnd, String separator) {
		return("");
	}
	
	/*
	 * export Entity to CSV format
	 * only text data would be exported
	 */
	public String toHtml() {
		return("");
	}
	
	/*
	 * export Entity to CSV format
	 * only text data would be exported
	 */
	public String toText() {
		return("");
	}
	
	/*
	 * export Entity to CSV format
	 * all data would be exported
	 */
	public String toXml() {
		return("");
	}
	
	public String xmlMeta(int n,String name, String value) {
		StringBuilder b=new StringBuilder();
		b.append(xmlTab(n)).append("<").append(name.toLowerCase()).append(">");
		if ((value!=null) && !value.isEmpty()) {
			if (!value.replace("<p>", "").replace("</p>", "").replace("\n", "").trim().isEmpty()) {
				b.append("<![CDATA[").append(value).append("]]>");
			}
		}
		b.append("</").append(name.toLowerCase()).append(">\n");
		return(b.toString());
	}
	public String xmlCommon() {
		String s=xmlAttribute("id", getId().toString());
		return(s);
	}
	public String xmlAttribute(String name, String value) {
		if (value==null) return(xmlTab(2)+name.toLowerCase()+"=\"\"\n");
		return(xmlTab(2)+name.toLowerCase()+"=\""+value.replace("\"", "''")+"\"\n");
	}

	public String xmlTab(int i) {
		if (i==0) return("");
		String b="";
		for (int k=0;k<i;k++) b+="    ";
		return(b);
	}

	public String getClean(Timestamp d) {
		return(d!=null?d.toString():"");
	}
	public String getClean(Date date) {
		return(date!=null?date.toString():"");
	}
	public String getClean(Integer d) {
		return(d!=null?d.toString():"");
	}
	public String getClean(Long d) {
		return(d!=null?d.toString():"");
	}
	public String getClean(String str) {
		if (str==null) return("");
		return(StringEscapeUtils.escapeHtml3(str));
	}
	public String getClean(Location location) {
		return(location!=null?location.getId().toString():"");
	}
	public String getClean(Person person) {
		return(person!=null?person.getId().toString():"");
	}
	public String getClean(Category cat) {
		return(cat!=null?cat.getId().toString():"");
	}
	public String getClean(Part part) {
		return(part!=null?part.getId().toString():"");
	}
	public String getClean(Gender gender) {
		return(gender!=null?gender.getId().toString():"");
	}
	public String getClean(Species species) {
		return(species!=null?species.getId().toString():"");
	}
	public String getClean(Chapter chapter) {
		return(chapter!=null?chapter.getId().toString():"");
	}
	public String getClean(Strand strand) {
		return(strand!=null?strand.getId().toString():"");
	}
	public String getCleanId(AbstractEntity entity) {
		return(entity!=null?entity.getId().toString():"");
	}
	public static String getXmlString(Node node, String n) {
		Element e=(Element)node;
		return(e.getAttribute(n).trim());
	}
	public static Long getXmlLong(Node node, String n) {
		String x=getXmlString(node,n);
		if (x.isEmpty()||"null".equals(x)) return(null);
		return(Long.parseLong(x));
	}
	public static Integer getXmlInteger(Node node, String n) {
		String x=getXmlString(node,n);
		if (x.isEmpty()||"null".equals(x)) return(null);
		return(Integer.parseInt(x));
	}
	public static Timestamp getXmlTimestamp(Node node, String n) {
		String ts = getXmlString(node, n);
		if (!ts.isEmpty()) {
			return(Timestamp.valueOf(ts));
		}
		return((Timestamp)null);
	}
	public static String getXmlText(Node node, String n) {
		NodeList t = node.getChildNodes();
		if (t.getLength()>0) {
			for (int i=0;i<t.getLength();i++) {
				if (t.item(i).getNodeName().equals(n)) {
					return(t.item(i).getTextContent().trim());
				}
			}
		}
		return("");
	}
	
	public static List<String> getXmlList(Node node, String n) {
		NodeList t = node.getChildNodes();
		List<String> list=new ArrayList<>();
		if (t.getLength()>0) {
			for (int i=0;i<t.getLength();i++) {
				if (t.item(i).getNodeName().equals(n)) {
					list.add(t.item(i).getTextContent().trim());
				}
			}
		}
		return(list);
	}
	
}
