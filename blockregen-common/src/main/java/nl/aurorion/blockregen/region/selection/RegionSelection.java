package nl.aurorion.blockregen.region.selection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;

@RequiredArgsConstructor
@AllArgsConstructor
public class RegionSelection {

    @Getter
    @Setter
    private Location first;

    @Getter
    @Setter
    private Location second;
}
