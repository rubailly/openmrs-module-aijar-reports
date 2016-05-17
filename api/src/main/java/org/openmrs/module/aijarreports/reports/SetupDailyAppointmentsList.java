package org.openmrs.module.aijarreports.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openmrs.api.context.Context;
import org.openmrs.module.aijarreports.library.ARTClinicCohortDefinitionLibrary;
import org.openmrs.module.aijarreports.library.BasePatientDataLibrary;
import org.openmrs.module.aijarreports.library.DataFactory;
import org.openmrs.module.aijarreports.library.HIVPatientDataLibrary;
import org.openmrs.module.aijarreports.metadata.CommonReportMetadata;
import org.openmrs.module.aijarreports.metadata.HIVMetadata;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.patient.library.BuiltInPatientDataLibrary;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Daily Appointments List report
 */
@Component
public class SetupDailyAppointmentsList extends AijarDataExportManager {

	@Autowired
	ARTClinicCohortDefinitionLibrary hivCohorts;

	@Autowired
	private DataFactory df;

	@Autowired
	private CommonReportMetadata commonMetadata;

	@Autowired
	private HIVMetadata hivMetadata;

	private BuiltInPatientDataLibrary builtInPatientData;

	@Autowired
	private HIVPatientDataLibrary hivPatientData;

	@Autowired
	private BasePatientDataLibrary basePatientData;

	/**
	 * @return the uuid for the report design for exporting to Excel
	 */
	@Override
	public String getExcelDesignUuid() {
		return "98e9202d-8c00-415f-9882-43917181f08d";
	}

	@Override
	public String getUuid() {
		return "9c85e206-c3cd-4dc1-b332-13f1d02f1ccc";
	}

	@Override
	public String getName() {
		return "Daily Appointments List";
	}

	@Override
	public String getDescription() {
		return "A list of clients expected for appointments in the ART clinic on the specified date";
	}

	@Override
	public List<Parameter> getParameters() {
		List<Parameter> l = new ArrayList<Parameter>();
		l.add(df.getEndDateParameter());
		return l;
	}

	@Override
	public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
		List<ReportDesign> l = new ArrayList<ReportDesign>();
		l.add(createExcelTemplateDesign(getExcelDesignUuid(), reportDefinition, "DailyAppointmentsList.xls"));
		return l;
	}

	@Override
	public ReportDefinition constructReportDefinition() {
		ReportDefinition rd = new ReportDefinition();
		rd.setUuid(getUuid());
		rd.setName(getName());
		rd.setDescription(getDescription());
		rd.setParameters(getParameters());

		PatientDataSetDefinition dsd = new PatientDataSetDefinition();
		dsd.setName("DAL");
		dsd.setParameters(getParameters());

		// rows are patients with a next appointment date obs in the given date range
		CohortDefinition rowFilter = hivCohorts.getPatientsWithAppointmentOnDate();
		dsd.addRowFilter(Mapped.mapStraightThrough(rowFilter));

		// columns to include
		addColumn(dsd, "ID", hivPatientData.getClinicNumber());
		addColumn(dsd, "familyName", builtInPatientData.getPreferredFamilyName());
		addColumn(dsd, "givenName", builtInPatientData.getPreferredGivenName());
		addColumn(dsd, "Sex", builtInPatientData.getGender());
		addColumn(dsd, "Birthdate", builtInPatientData.getBirthdate());

		rd.addDataSetDefinition(getName(), Mapped.mapStraightThrough(dsd));

		ReportDesign design;
		try {
			design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "DailyAppointmentsList.xls", "DailyAppointmentsList.xls_", null);
			Properties props = new Properties();
			props.put("repeatingSections", "sheet:1,row:9,dataset:DAL");
			props.put("sortWeight","5000");
			design.setProperties(props);

			Helper.saveReportDesign(design);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	

		return rd;
	}

	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("DailyAppointmentsList.xls_".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("Daily Appointments List");
	}
	@Override
	public String getVersion() {
		return "0.1";
	}
}
