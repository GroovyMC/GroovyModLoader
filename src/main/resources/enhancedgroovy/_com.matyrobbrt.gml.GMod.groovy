//file:noinspection GrPackage
import com.matyrobbrt.enhancedgroovy.dsl.ClassTransformer

((ClassTransformer) this.transformer).tap {
    it.addField([
            'name': 'modBus',
            'type': 'com.matyrobbrt.gml.bus.GModEventBus',
            'modifiers': ['private', 'final']
    ])
    it.addField([
            'name': 'forgeBus',
            'type': 'net.minecraftforge.eventbus.api.IEventBus',
            'modifiers': ['private', 'final']
    ])
}