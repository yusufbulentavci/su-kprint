package kexamprint;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.pdfbox.pdmodel.PDPage;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class SoruKagidi extends RecurringPageDocument {

	private static final String DIR = "/home/rompg/Downloads/a/final/exams/";
	private static final String IMGDIR = "/home/rompg/Downloads/a/final/resim/";
	private static final String examName = "BAHORGI SEMESTR YAKUNIY NAZORAT";
	private static final String tableName = "final.jadvalhazir";
	private static final String DAYS = "'12'";



	private static final String SQL_SELECT = "select  "
			+ "(case when qolgan then 'Qayta o''qish-' else '' end)||"
			+ "(case when sirtqi='sirtqi' then 'SIRTQI/' else '' end)"
			+ "||fan_kodi||'-'||curriculum_language||'["+examName+"]',\n"
			+ "			  sorupk, \n"
			+ "			  fan, \n"
			+ "			  auditoriya||'-'||stul||'\n'||date||' '||kun||' '||vaqt||'\n'||coalesce(curriculum_bylanguage_id,'')||'-'||coalesce(curriculum_year,''),\n"
			+ "			  exam_img,\n"
			+ "			  kun,\n"
			+ "			  auditoriya,\n"
			+ "			  vaqt,stul,date,exam\n"
			+ "			  from "+tableName+" v \n"
			+ "        where exam<>'design' and trim(iday) in ("+DAYS+") \n"
			+ "\n"
			+ "";
			
	
	static int count=0;
	static int bos=0;
	
	public static void main(String[] args) throws Exception {
//		FileUtils.deleteDirectory(new File(DIR));	

		try (Connection conn = DriverManager.getConnection("jdbc:postgresql://sdb:5432/kampusv2", "krapp",
				"+SamBtg2024"); 
			PreparedStatement preparedStatement = conn.prepareStatement(SQL_SELECT)) {
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next()) {
				String h11 = resultSet.getString(1);
				String h12 = resultSet.getString(2);
				String h21 = resultSet.getString(3);
				String h22 = resultSet.getString(4);
				String img = resultSet.getString(5);
				String dayuz = resultSet.getString(6);
				String room = resultSet.getString(7);
				String hours = resultSet.getString(8);
				Integer sira=resultSet.getInt(9);
				String tarih = resultSet.getString(10);
				String examType = resultSet.getString(11);
				if(img==null) {
					bos++;
					System.out.println("Bos:"+bos);
//					continue;
				}
				count++;
//				System.out.println(count);
				
				SoruKagidi q = new SoruKagidi(h11, h12, h21, h22, img, dayuz, room, hours, sira, tarih, examType);
				q.render();
			}
		} finally {
		}

	}

	final String h11;
	final String h12;
	final String h21;
	String h22;
	final String img;
	String dayuz;
	String room;
	String hours;
	Integer sira;


	private String tarih;


	private String examType;



	public SoruKagidi(String h11, String h12, String h21, String h22, String img, String dayuz, String room, String hours, 
			Integer sira, String tarih, String examType) {
		super();
		this.h11 = h11;
		this.h12 = h12;
		this.h21 = h21;
		this.h22 = h22;
		this.img = img;
		this.dayuz=dayuz;
		this.room=room;
		this.hours=hours;
		this.sira=sira;
		this.tarih=tarih;
		this.examType=examType;
	}

//	@Override
//	public String getFolder() {
//		return "/home/rompg/Downloads/vize2/output/exams/"+dayuz+"/"+hours+"/"+room;
//	}
	@Override
	public String getFolder() {
		String t=tarih.replace('/', '-');
		return DIR+t+"-"+dayuz+"/"+hours+"/"+room+"/";
	}

	@Override
	public String getFile() {
		return room+"-"+dayuz+"-"+hours+"--"+sira+(isA4()?"":"-a3")+".pdf";
	}

	@Override
	public boolean next() {
		return false;
	}

	@Override
	public
	void addHeader(PdfWriter writer, Document doc, PDPage page) throws DocumentException {
		PdfPTable table = new PdfPTable(2);
		table.setWidths(new float[] { 5, 2 });

		table.setWidthPercentage(100);

		Font bold = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
		Font font = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

		PdfPCell cell = new PdfPCell(new com.itextpdf.text.Paragraph(this.h11, bold));
		cell.setPadding(5);
		table.addCell(cell);
//		if (isHeader) {
//			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//		}
		cell = new PdfPCell(new com.itextpdf.text.Paragraph(this.h12, bold));
		cell.setPadding(5);
		table.addCell(cell);
		cell = new PdfPCell(new com.itextpdf.text.Paragraph(this.h21, bold));
		cell.setPadding(5);
		table.addCell(cell);
		cell = new PdfPCell();
		if(h22==null)
			h22="";
		String[] ss = h22.split("\n");
		for (String string : ss) {
			cell.addElement(new Paragraph(string, font));
		}
		cell.setPadding(5);
		table.addCell(cell);
		doc.add(table);

	}

	public boolean isA4() {
		return !this.examType.equals("drawing");
	}
	
	@Override
	public	void addBody(PdfWriter writer, Document doc, PDPage page)
			throws DocumentException, MalformedURLException, IOException {
		if(this.img==null||!(new File(IMGDIR + this.img.replace('\\', '/')).exists())) {
			System.out.println("Exam absent Day:"+this.dayuz+" Hours:"+this.hours+" Room:"+this.room+" Stul:"+this.sira);
			return;
		}
//		System.out.println(this.img);
		Image image = Image.getInstance(IMGDIR + this.img.replace('\\', '/'));
		if(isA4()) {
			image.scaleToFit(PageSize.A4.getWidth() - 63, PageSize.A4.getHeight() - 50); // Scale image to fit
		}else {
			image.scaleToFit(PageSize.A3.getWidth() - 63, PageSize.A3.getHeight() - 50); 
		}
		
		
		image.setAlignment(Image.ALIGN_CENTER); // Center alignment

		doc.add(image);

	}

	@Override
	public
	void addPageHeader(PdfWriter writer, Document document) {
		// TODO Auto-generated method stub

	}

	@Override
	public
	void addFooter(PdfWriter writer, Document doc, PDPage page) throws Exception {
	}

	@Override
	public
	void addPageFooter(PdfWriter writer, Document document) {
		Font largeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
		Chunk largeText = new Chunk("Imtihon Baholovchisi:________________________", largeFont);
		Paragraph paragraph = new Paragraph();
		paragraph.add(largeText);

		PdfContentByte cb = writer.getDirectContent();

		ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, paragraph, document.leftMargin(), document.bottom() - 10, 0);

	}

}
