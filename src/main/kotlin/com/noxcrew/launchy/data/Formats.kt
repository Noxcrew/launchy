package com.noxcrew.launchy.data

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

object Formats {
    val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )
}
