
package com.gpal.DaemonPalomino.builders;

import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.app.VelocityEngine;

import com.gpal.DaemonPalomino.models.DBDocument;
import com.gpal.DaemonPalomino.models.PendingDocument;
import com.gpal.DaemonPalomino.utils.DataUtil;
import javax.sql.DataSource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateDocument {

	private VelocityEngine velocityEngine;

	@Inject
	public GenerateDocument(@Named("velocity") VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	public void generateDocument(int sizeBatch, DataSource dataSource) {

		List<Object> input = new ArrayList<>();
		input.add(sizeBatch);
		input.add("001"); // for only BOL

		// Obtaining restant documents
		List<PendingDocument> b = DataUtil.executeProcedure(dataSource, "EXEC SP_TTHELP_DOCU01 ?,?", input,
				PendingDocument.class);

		b.forEach(data -> {
			generateXMLUnsigned(dataSource, data);
		});
		log.info("The size: " + b.toString());

	}

	public void generateXMLUnsigned(DataSource dataSource, PendingDocument pendingDocument) {
		List<Object> input = new ArrayList<>();
		input.add(pendingDocument.getNU_DOCU());
		input.add(pendingDocument.getTI_DOCU());
		input.add(pendingDocument.getCO_EMPR());
		input.add(pendingDocument.getCO_ORIG());

		if (pendingDocument.getTI_DOCU().equals("BOL")) {

			List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC TH_OBT_DOCU ?,?,?,?", input,
					DBDocument.class);

			DBDocument document = dbDocuments.get(0);

		} else if (pendingDocument.getTI_DOCU().equals("FAC")) {

			List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC TH_OBT_DOCU ?,?,?,?", input,
					DBDocument.class);

			DBDocument document = dbDocuments.get(0);

		} else if (pendingDocument.getTI_DOCU().equals("NCR")) {

			List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC TH_OBT_DOCU ?,?,?,?", input,
					DBDocument.class);

			DBDocument document = dbDocuments.get(0);

		} else if (pendingDocument.getTI_DOCU().equals("NCD")) {

			List<DBDocument> dbDocuments = DataUtil.executeProcedure(dataSource, "EXEC TH_OBT_DOCU ?,?,?,?", input,
					DBDocument.class);

			DBDocument document = dbDocuments.get(0);

		} else {
			log.info("Tipo de documento no identificado...");
		}
	}

}
