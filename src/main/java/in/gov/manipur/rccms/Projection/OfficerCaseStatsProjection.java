package in.gov.manipur.rccms.Projection;

public interface OfficerCaseStatsProjection {

    String getName();
    String getDesignation();
    String getDistrict();

    Long getPending();
    Long getDisposed();
    Long getTotalCases();
}
