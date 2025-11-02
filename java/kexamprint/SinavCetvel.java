package kexamprint;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDPage;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;




public class SinavCetvel extends RecurringPageDocument {

	private static final String DIR = "/home/rompg/Downloads/a/final/exams/";
	private static final String tableName = "final.jadvalhazir";
	private static final String DAYS = "'12'";
	String dayUz;

	public SinavCetvel(String auditoriya, String tarih, String hours, String dayUz) {
		super();
		this.auditoriya = auditoriya;
		this.tarih = tarih;
		this.hours = hours;
		this.dayUz=dayUz;
	}

	String auditoriya;
	String tarih;
	String hours;
//	String folder;
	
	private List<Item> list=new ArrayList<Item>();

	
	private static final String SQL_SELECT="select fan_kodi,auditoriya,date,vaqt,coalesce(stul::int,0) as ssira,\n"
			+ "			          v.group_code,v.student_name,v.student_surname,v.student_id,kun,qolgan,sirtqi\n"
			+ "		from "+tableName+" v\n"
			+ "    where v.exam<>'design' and stul<>''\n"
			+ "     and v.auditoriya is not null and trim(iday) in ("+DAYS+") \n"
			+ "		 order by date,v.auditoriya,v.kun,v.vaqt,v.stul::int\n"
			+ "\n"
			+ "";
	private static final String examName = "BAHORGI SEMESTR YAKUNIY NAZORAT";

	static boolean ql=true;
	static boolean qs=true;
	
	
	public static void main(String[] args) throws Exception {
		
		FileUtils.deleteDirectory(new File(DIR));

		try (Connection conn = DriverManager.getConnection("jdbc:postgresql://sdb:5432/kampusv2", "krapp",
				"+SamBtg2024"); PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
			ResultSet resultSet = preparedStatement.executeQuery();

			String oldy=null;
			SinavCetvel ek=null;
			while (resultSet.next()) {
				String fanKodi = resultSet.getString(1);
				String auditoriya = resultSet.getString(2);
				String tarih = resultSet.getString(3);
				String hours = resultSet.getString(4);
				String dosya=auditoriya+tarih+hours;
				String sira = resultSet.getString(5);
				String groupCode = resultSet.getString(6);
				String sName = resultSet.getString(7);
				String sSurname = resultSet.getString(8);
				String sId = resultSet.getString(9);
				String dayUz = resultSet.getString(10);
				boolean qolgan=resultSet.getBoolean(11);
				String sirtqi = resultSet.getString(12);

				if(ql && qolgan) {
					ql=false;
					System.out.println(hours+"-qolgan-"+auditoriya+sira);
				}
				if(qs && sirtqi.equals("sirtqi")) {
					qs=false;
					System.out.println(hours+"-sirtqi-"+auditoriya+sira);
				}
				
				if(oldy==null || !dosya.equals(oldy)) {
					if(oldy!=null) {
						System.out.println("o:"+oldy);
						System.out.println("d:"+dosya);
						ek.render();
					}
					ek = new SinavCetvel(auditoriya, tarih, hours, dayUz);
					oldy=dosya;
				}
				
				ek.addItem(Integer.parseInt(sira),groupCode,sName,sSurname,sId);
			}
			System.out.println("r:"+oldy);
			ek.render();
		} finally {
		}

	}

	private void addItem(Integer sira2, String groupCode2, String sName2, String sSurname2, String sId2) {
		this.list.add(new Item(sira2, groupCode2, sName2, sSurname2, sId2));
	}

	@Override
	public String getFile() {
		String t=tarih.replace('/', '-');
		return "_imzo_"+auditoriya+"-"+t+"-"+dayUz+"-"+hours+".pdf";
	}
	@Override
	public String getFolder() {
		String t=tarih.replace('/', '-');
		return DIR+t+"-"+dayUz+"/"+hours+"/"+auditoriya+"/";
	}
	


	@Override
	public boolean next() {
		return false;
	}

	@Override
	public
	void addHeader(PdfWriter writer, Document doc, PDPage page) throws DocumentException {
		Font largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
		Chunk largeText = new Chunk("Nazoratchi:__________________________________   Imzo: ", largeFont);
		Paragraph paragraph = new Paragraph();
		paragraph.add(largeText);
		doc.add(paragraph);
		doc.add(new com.itextpdf.text.Paragraph("\n")); // Add space after header

	}

	@Override
	public
	void addBody(PdfWriter writer, Document doc, PDPage page)
			throws DocumentException, MalformedURLException, IOException {
		PdfPTable table = new PdfPTable(4);
		table.setWidths(new float[] { 2,1,2,1 });

		table.setWidthPercentage(100);

		boolean eksik=false;
		for(Item i:list) {
			
			Font largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
			Chunk largeText = new Chunk(i.sId+" "+i.sSurname+" "+i.sName, largeFont);
			Font smallFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
			Chunk smallText = new Chunk("\n"+i.groupCode, smallFont);
			
			
			Paragraph paragraph = new Paragraph();
			paragraph.add(largeText);
			paragraph.add(smallText);

			PdfPCell cell = new PdfPCell(paragraph);
//			cell.enableBorderSide(PdfPCell.BOTTOM);
//			cell.enableBorderSide(PdfPCell.TOP);
//			cell.setBorder(PdfPCell.BOTTOM);
			cell.setPadding(5);
//			cell.setFixedHeight(670);
			table.addCell(cell);

			largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
			largeText = new Chunk(i.sId+" "+i.sSurname+" "+i.sName+"\n"+i.groupCode, largeFont);
			paragraph = new Paragraph();
			paragraph.add(i.sira==0?"":i.sira+"");

			cell = new PdfPCell(paragraph);
			
			cell.setPadding(5);
			table.addCell(cell);

			eksik=!eksik;
			
		}
		if(eksik) {
			table.addCell(new PdfPCell(new Paragraph("")));
			table.addCell(new PdfPCell(new Paragraph("")));
		}
		doc.add(table);
		System.out.println(getFolder()+"/"+getFile()+":"+list.size());
	}

	@Override
	public
	void addFooter(PdfWriter writer, Document doc, PDPage page) throws Exception {
//		Font largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
//		Chunk largeText = new Chunk("Imtihon Baholovchisi:________________________", largeFont);
//		Paragraph paragraph = new Paragraph();
//		paragraph.add(largeText);
//
////        doc.add(paragraph);
//		PdfContentByte cb = writer.getDirectContent();
////		Phrase header = new Phrase("this is a header", ffont);
////		Phrase footer = new Phrase("this is a footer", ffont);
////		ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, paragraph,
////				(doc.right() - doc.left()) / 2 + doc.leftMargin(), doc.top() + 10, 0);
//
//		ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, paragraph, doc.leftMargin(), doc.bottom() - 10, 0);

	}

	@Override
	public
	void addPageFooter(PdfWriter writer, Document document) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public
	void addPageHeader(PdfWriter writer, Document document) {
		Font largeFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
		Chunk largeText = new Chunk(examName+" IMZO RO'YXATI ", largeFont);
		Font normalFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
		Chunk normalText = new Chunk("AUDITORIYA: "+this.auditoriya+"-"+tarih+" "+this.dayUz+" "+hours, normalFont);

		
		Paragraph pl = new Paragraph();
		pl.add(largeText);

		Paragraph pr = new Paragraph();
		
		pr.add(normalText);

		
		
		PdfContentByte cb = writer.getDirectContent();
		
		ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, pl,
				document.leftMargin(), document.top() + 5, 0);

		ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pr,
				document.right(), document.top() + 5, 0);

	}

}
