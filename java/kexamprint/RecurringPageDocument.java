package kexamprint;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDPage;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
public abstract class RecurringPageDocument extends PdfPageEventHelper {

	public enum PageSizeType {
		A4(PageSize.A4), A3(PageSize.A3);

		private final Rectangle size;

		PageSizeType(Rectangle size) {
			this.size = size;
		}

		public Rectangle getSize() {
			return this.size;
		}
	}

	private PageSizeType pageSizeType;
	
	public boolean isA4() {
		return true;
	}
	
	@Override
	public void onEndPage(PdfWriter writer, Document document) {

		try {
			addPageHeader(writer, document);
			addPageFooter(writer, document);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public abstract void addPageFooter(PdfWriter writer, Document document);

	public abstract void addPageHeader(PdfWriter writer, Document document);

	public void render() throws Exception {
		this.pageSizeType=isA4()?PageSizeType.A4:PageSizeType.A3;

		Document document = new Document(pageSizeType.getSize());
		
		File folder = new File(getFolder());
		folder.mkdirs();
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(new File(folder, getFile())));
		writer.setPageEvent(this);

		document.open();
		do {
			PDPage page = setUpPage(document);
//			PDPageContentStream contentStream = new PDPageContentStream(document, page);
			addHeader(writer, document, page);
			addBody(writer, document, page);
//			addFooter(writer, document, page);
//			contentStream.close();
		} while (next());
//		saveFile(document, getFile());
		document.close();
	}

	public abstract String getFolder();

	public void saveFile(Document document, String file) throws IOException {
//		document.save(file);
	}

	public abstract String getFile();

	public abstract boolean next();

	public PDPage setUpPage(Document document) {
		PDPage page = new PDPage();
		return page;
	}

	public abstract void addHeader(PdfWriter writer, Document doc, PDPage page) throws Exception;

	public abstract void addBody(PdfWriter writer, Document doc, PDPage page) throws Exception;

	public abstract void addFooter(PdfWriter writer, Document doc, PDPage page) throws Exception;

}
