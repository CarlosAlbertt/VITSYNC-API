package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.Informe;
import com.ejemplo.vitsync.model.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class InformePdfService {

    private static final float MARGIN = 55f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(Informe informe, User paciente, User medico) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("VitSync - Informe Medico");
                cs.endText();
                y -= 10;

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 22;

                // Datos del informe
                y = writeSectionTitle(cs, "Datos del informe", y);
                y = writeLabelValue(cs, "Titulo", orDash(informe.getTitulo()), y);
                y = writeLabelValue(cs, "Tipo", orDash(informe.getTipo()), y);
                y = writeLabelValue(cs, "Fecha",
                        informe.getFecha() != null ? informe.getFecha().format(DATE_FMT) : "-", y);
                y -= 14;

                // Paciente
                if (paciente != null) {
                    y = writeSectionTitle(cs, "Paciente", y);
                    y = writeLabelValue(cs, "Nombre", fullName(paciente), y);
                    y = writeLabelValue(cs, "NIF", orDash(paciente.getNif()), y);
                    y -= 14;
                }

                // Medico
                if (medico != null) {
                    y = writeSectionTitle(cs, "Medico responsable", y);
                    y = writeLabelValue(cs, "Nombre", fullName(medico), y);
                    y -= 14;
                }

                // Notas personales
                if (informe.getNotasPersonales() != null && !informe.getNotasPersonales().isBlank()) {
                    y = writeSectionTitle(cs, "Notas personales", y);
                    writeWrappedText(cs, informe.getNotasPersonales(), y);
                }

                // Footer
                cs.setLineWidth(0.3f);
                cs.moveTo(MARGIN, MARGIN + 16);
                cs.lineTo(PAGE_WIDTH - MARGIN, MARGIN + 16);
                cs.stroke();
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.newLineAtOffset(MARGIN, MARGIN);
                cs.showText("Generado el " + LocalDate.now().format(DATE_FMT) + " - VitSync");
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private float writeSectionTitle(PDPageContentStream cs, String title, float y) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(title);
        cs.endText();
        return y - 16;
    }

    private float writeLabelValue(PDPageContentStream cs, String label, String value, float y) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(label + ":");
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(MARGIN + 110, y);
        cs.showText(value);
        cs.endText();

        return y - 14;
    }

    private float writeWrappedText(PDPageContentStream cs, String text, float y) throws IOException {
        int maxChars = 88;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > maxChars) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(line.toString().trim());
                cs.endText();
                y -= 14;
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(line.toString().trim());
            cs.endText();
            y -= 14;
        }
        return y;
    }

    private String fullName(User user) {
        String n = (user.getName() != null ? user.getName() : "") + " "
                + (user.getFirstName() != null ? user.getFirstName() : "");
        return n.trim();
    }

    private String orDash(String value) {
        return (value != null && !value.isBlank()) ? value : "-";
    }
}
