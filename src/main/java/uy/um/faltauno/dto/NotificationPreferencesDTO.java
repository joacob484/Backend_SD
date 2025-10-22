package uy.um.faltauno.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesDTO {
    
    @JsonProperty("matchInvitations")
    private Boolean matchInvitations;
    
    @JsonProperty("friendRequests")
    private Boolean friendRequests;
    
    @JsonProperty("matchUpdates")
    private Boolean matchUpdates;
    
    @JsonProperty("reviewRequests")
    private Boolean reviewRequests;
    
    @JsonProperty("newMessages")
    private Boolean newMessages;
    
    @JsonProperty("generalUpdates")
    private Boolean generalUpdates;
}
