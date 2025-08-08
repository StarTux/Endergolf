package com.cavetale.endergolf.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data
@NotNull
@Name("best")
@UniqueKey(value = {"mapPath", "player"}, name = "map_player")
public final class SQLMapPlayerBest implements SQLRow {
    @Id private Integer id;
    private String mapPath;
    private UUID player;
    private int strokes;
    private Date time;

    public SQLMapPlayerBest() { }

    public SQLMapPlayerBest(final String mapPath, final UUID player, final int strokes, final Date time) {
        this.mapPath = mapPath;
        this.player = player;
        this.strokes = strokes;
        this.time = time;
    }
}
