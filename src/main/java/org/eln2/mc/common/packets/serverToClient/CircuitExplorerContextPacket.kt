package org.eln2.mc.common.packets.serverToClient

import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.network.NetworkEvent
import org.eln2.mc.Eln2
import org.eln2.mc.client.gui.CellInfo
import org.eln2.mc.client.gui.PlotterScreen
import java.util.function.Supplier

class CircuitExplorerContextPacket() {
    lateinit var cells : ArrayList<CellInfo>

    constructor(buffer : FriendlyByteBuf) : this(){
        val count = buffer.readInt()

        if(count == 0){
            Eln2.LOGGER.error("ERROR! Received 0 cells for the circuit explorer!")
            return
        }

        cells = ArrayList(buffer.readCollection({ArrayList<CellInfo>(count)}, ::CellInfo))
    }

    constructor(cells : ArrayList<CellInfo>) : this(){
        this.cells = cells
    }

    companion object {
        fun encode(packet : CircuitExplorerContextPacket, buffer : FriendlyByteBuf){
            buffer.writeInt(packet.cells.count())
            buffer.writeCollection(packet.cells) { pBuffer, info -> info.serialize(pBuffer)}
        }

        fun handle(packet : CircuitExplorerContextPacket, supplier : Supplier<NetworkEvent.Context>){
            supplier.get().enqueueWork {
                if(Dist.CLIENT == FMLEnvironment.dist){
                    handleClient(packet, Minecraft.getInstance())
                }
            }
        }

        private fun handleClient(packet : CircuitExplorerContextPacket, mc : Minecraft){
            if(mc.screen is PlotterScreen){
                Eln2.LOGGER.error("Screen already set!")
                return
            }

            mc.setScreen(PlotterScreen(packet.cells))

            Eln2.LOGGER.info("Received explorer packet and handled it!")
        }
    }
}
