package org.eln2.mc.common.cell

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import org.eln2.mc.Eln2
import org.eln2.mc.common.In
import org.eln2.mc.common.Side
import java.lang.IndexOutOfBoundsException
import java.util.*

@In(Side.LogicalServer)
class CellGraphManager(val level : Level) : SavedData() {
    private val graphs = HashMap<UUID, CellGraph>()

    // TODO: Is this still useful?
    fun containsGraph(graph : CellGraph) : Boolean{
        return graphs.containsKey(graph.id)
    }

    fun containsGraphWithId(id : UUID) : Boolean{
        return graphs.containsKey(id)
    }

    fun addGraph(graph : CellGraph) {
        graphs[graph.id] = graph
        //_logger.info("Added graph ${graph.id}!")
        setDirty()
    }

    fun removeGraph(graph : CellGraph) {
        graphs.remove(graph.id)
        Eln2.LOGGER.info("Removed graph ${graph.id}!")
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val graphListTag = ListTag()
        graphs.values.forEach { graph->
            graphListTag.add(graph.toNbt())
        }
        tag.put("Graphs", graphListTag)
        Eln2.LOGGER.info("Wrote ${graphs.count()} graphs to disk.")
        return tag
    }

    fun getGraphWithId(id : UUID) : CellGraph{
        return graphs[id]?: throw IndexOutOfBoundsException("Graph ID was not found in the cell graph ${graphs}: $id")
    }

    companion object {
        private fun load(tag : CompoundTag, level : ServerLevel) : CellGraphManager {
            val manager = CellGraphManager(level)

            val graphListTag = tag.get("Graphs") as ListTag?

            if(graphListTag == null){
                Eln2.LOGGER.info("No nodes to be loaded!")
                return manager
            }

            graphListTag.forEach { circuitNbt ->
                val graphCompound  = circuitNbt as CompoundTag
                val graph = CellGraph.fromNbt(graphCompound, manager)
                if(graph.cells.isEmpty()){
                   Eln2.LOGGER.error("Loaded circuit with no cells!")
                    return@forEach
                }

                manager.addGraph(graph)
                Eln2.LOGGER.info("Loaded ${graph.cells.count()} cells for ${graph.id}!")
            }
            return manager
        }

        fun getFor(level : ServerLevel) : CellGraphManager {
            // this will return the instance or create a new one
            return level.dataStorage.computeIfAbsent({ load(it, level) }, { CellGraphManager(level) }, "CellManager")
        }
    }
}
