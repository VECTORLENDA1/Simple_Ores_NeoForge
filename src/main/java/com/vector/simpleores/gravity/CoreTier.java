package com.vector.simpleores.gravity;

import net.minecraft.util.StringRepresentable;

/// Os 4 niveis (tiers) do Nucleo Gravitacional.
/// Para mudar o aspeto ou o comportamento de um tier, basta mudar os numeros aqui.
///
/// Cores: formato 0xAARRGGBB (AA = transparencia, FF = opaco).
public enum CoreTier implements StringRepresentable {
    //          nome          nivel  cor do nucleo  cor do brilho  tamanho  raio orbita  velocidade  max itens  luz
    SUN(        "sun",        1,     0xFFFFD34D,    0xFFFF8A00,    1.0f,    1.2f,        1.0f,       6,         15),
    RED_GIANT(  "red_giant",  2,     0xFFC62A12,    0xFFFF4A1C,    1.3f,    1.4f,        0.7f,       9,         12),
    PULSAR(     "pulsar",     3,     0xFFEAF6FF,    0xFF2E9BFF,    1.6f,    1.6f,        3.0f,       12,        15),
    BLACK_HOLE( "black_hole", 4,     0xFF000000,    0xFF8A2BE2,    2.0f,    1.9f,        2.0f,       16,        4);

    /// Nome usado nos IDs (ex: "gravity_core_sun").
    public final String id;
    /// Nivel do tier. Uma receita com "tier": 2 so funciona em nucleos de nivel 2 ou superior.
    public final int level;
    /// Cor da esfera do nucleo.
    public final int coreColor;
    /// Cor da "aura" transparente a volta do nucleo.
    public final int glowColor;
    /// Diametro da esfera em blocos (1.0 = um bloco inteiro).
    public final float coreSize;
    /// Distancia (em blocos) entre o centro do nucleo e os itens em orbita.
    public final float orbitRadius;
    /// Velocidade da orbita (1.0 = normal, 2.0 = o dobro).
    public final float orbitSpeed;
    /// Quantos tipos de itens diferentes podem estar em orbita ao mesmo tempo.
    public final int maxItems;
    /// Luz emitida pelo bloco (0 a 15).
    public final int light;

    CoreTier(String id, int level, int coreColor, int glowColor, float coreSize, float orbitRadius,
             float orbitSpeed, int maxItems, int light) {
        this.id = id;
        this.level = level;
        this.coreColor = coreColor;
        this.glowColor = glowColor;
        this.coreSize = coreSize;
        this.orbitRadius = orbitRadius;
        this.orbitSpeed = orbitSpeed;
        this.maxItems = maxItems;
        this.light = light;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
