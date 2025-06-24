package ai.shreds.application.dtos;

import java.util.List;

public class ApplicationInventoryCheckResponseDTO {

    private List<ApplicationInventoryResultDTO> results;
    private boolean allAvailable;

    public ApplicationInventoryCheckResponseDTO() {
    }

    public ApplicationInventoryCheckResponseDTO(List<ApplicationInventoryResultDTO> results, boolean allAvailable) {
        this.results = results;
        this.allAvailable = allAvailable;
    }

    public List<ApplicationInventoryResultDTO> getResults() {
        return results;
    }

    public void setResults(List<ApplicationInventoryResultDTO> results) {
        this.results = results;
    }

    public boolean isAllAvailable() {
        return allAvailable;
    }

    public void setAllAvailable(boolean allAvailable) {
        this.allAvailable = allAvailable;
    }
}
